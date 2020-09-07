package hep.dataforge.context

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.provider.Provider
import hep.dataforge.provider.top
import hep.dataforge.values.Value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import mu.KLogger
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName

/**
 * The local environment for anything being done in DataForge framework. Contexts are organized into tree structure with [Global] at the top.
 * Context has [properties] - equivalent for system environment values, but grouped into a tree and inherited from parent context.
 *
 * The main function of the Context is to provide [PluginManager] which stores the loaded plugins and works as a dependency injection point.
 * The normal behaviour of the [PluginManager] is to search for a plugin in parent context if it is not found in a current one. It is possible to have
 * different plugins with the same interface in different contexts in the hierarchy. The usual behaviour is to use nearest one, but it could
 * be overridden by plugin implementation.
 *
 * Since plugins could contain mutable state, context has two states: active and inactive. No changes are allowed to active context.
 * @author Alexander Nozik
 */
public open class Context(
    final override val name: Name,
    public val parent: Context? = Global,
) : Named, MetaRepr, Provider, CoroutineScope {

    private val config = Config()

    /**
     * Context properties. Working as substitute for environment variables
     */
    private val properties: Meta = if (parent == null) {
        config
    } else {
        Laminate(config, parent.properties)
    }

    /**
     * Context logger
     */
    public val logger: KLogger = KotlinLogging.logger(name.toString())

    /**
     * A [PluginManager] for current context
     */
    public val plugins: PluginManager by lazy { PluginManager(this) }

    private val activators = HashSet<Any>()

    /**
     * Defines if context is used in any kind of active computations. Active context properties and plugins could not be changed
     */
    public val isActive: Boolean = activators.isNotEmpty()

    override val defaultTarget: String get() = Plugin.PLUGIN_TARGET

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            Value.TYPE -> properties.sequence().toMap()
            Plugin.PLUGIN_TARGET -> plugins.sequence(true).associateBy { it.name }
            else -> emptyMap()
        }
    }

    /**
     * Mark context as active and used by [activator]
     */
    public fun activate(activator: Any) {
        activators.add(activator)
    }

    /**
     * Mark context unused by [activator]
     */
    public fun deactivate(activator: Any) {
        activators.remove(activator)
    }

    /**
     * Change the properties of the context. If active, throw an exception
     */
    public fun configure(action: Config.() -> Unit) {
        if (isActive) error("Can't configure active context")
        config.action()
    }

    open override val coroutineContext: CoroutineContext by lazy {
        (parent ?: Global).coroutineContext.let { parenContext ->
            parenContext + SupervisorJob(parenContext[Job])
        }
    }

    /**
     * Detach all plugins and terminate context
     */
    public open fun close() {
        if (isActive) error("Can't close active context")
        //detach all plugins
        plugins.forEach { it.detach() }
    }

    override fun toMeta(): Meta = Meta {
        "parent" to parent?.name
        "properties" put properties.seal()
        "plugins" put plugins.map { it.toMeta() }
    }
}

/**
 * A map of all objects provided by plugins with given target and type
 */
@JvmName("typedContent")
public inline fun <reified T : Any> Context.resolve(target: String): Map<Name, T> = plugins.flatMap { plugin ->
    plugin.top<T>(target).entries.map { (plugin.name + it.key) to it.value }
}.associate { it }


public fun Context.resolve(target: String): Map<Name, Any> = resolve<Any>(target)

/**
 * The interface for something that encapsulated in context
 *
 */
public interface ContextAware {
    /**
     * Get context for this object
     *
     * @return
     */
    public val context: Context

    public val logger: KLogger
        get() = if (this is Named) {
            KotlinLogging.logger((context.name + this.name).toString())
        } else {
            context.logger
        }

}