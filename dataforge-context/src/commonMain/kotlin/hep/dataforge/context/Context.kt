package hep.dataforge.context

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.sequence
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.provider.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import mu.KLogger
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

/**
 * The local environment for anything being done in DataForge framework. Contexts are organized into tree structure with [Global] at the top.
 * Context has [properties] - equivalent for system environment values, but grouped into a tree and inherited from parent context.
 *
 * The main function of the Context is to provide [PluginManager] which stores the loaded plugins and works as a dependency injection point.
 * The normal behaviour of the [PluginManager] is to search for a plugin in parent context if it is not found in a current one. It is possible to have
 * different plugins with the same interface in different contexts in the hierarchy. The usual behaviour is to use nearest one, but it could
 * be overridden by plugin implementation.
 *
 */
public open class Context(
    final override val name: Name,
    public val parent: Context?,
    meta: Meta,
) : Named, MetaRepr, Provider, CoroutineScope {

    /**
     * Context properties. Working as substitute for environment variables
     */
    private val properties: Laminate = if (parent == null) {
        Laminate(meta)
    } else {
        Laminate(meta, parent.properties)
    }

    /**
     * Context logger
     */
    public val logger: KLogger = KotlinLogging.logger(name.toString())

    /**
     * A [PluginManager] for current context
     */
    public val plugins: PluginManager by lazy { PluginManager(this) }

    @Deprecated("To be removed in favor of immutable plugins")
    private val activators = HashSet<Any>()

    /**
     * Defines if context is used in any kind of active computations. Active context properties and plugins could not be changed
     */
    @Deprecated("To be removed in favor of immutable plugins")
    public val isActive: Boolean = activators.isNotEmpty()

    /**
     * Mark context as active and used by [activator]
     */
    @Deprecated("To be removed in favor of immutable plugins")
    public fun activate(activator: Any) {
        activators.add(activator)
    }

    /**
     * Mark context unused by [activator]
     */
    @Deprecated("To be removed in favor of immutable plugins")
    public fun deactivate(activator: Any) {
        activators.remove(activator)
    }

    override val defaultTarget: String get() = Plugin.TARGET

    public fun content(target: String, inherit: Boolean): Map<Name, Any> {
        return if (inherit) {
            when (target) {
                PROPERTY_TARGET -> properties.sequence().toMap()
                Plugin.TARGET -> plugins.list(true).associateBy { it.name }
                else -> emptyMap()
            }
        } else {
            when (target) {
                PROPERTY_TARGET -> properties.layers.firstOrNull()?.sequence()?.toMap() ?: emptyMap()
                Plugin.TARGET -> plugins.list(false).associateBy { it.name }
                else -> emptyMap()
            }
        }
    }

    override fun content(target: String): Map<Name, Any>  = content(target,true)

    override val coroutineContext: CoroutineContext by lazy {
        (parent ?: Global).coroutineContext.let { parenContext ->
            parenContext + SupervisorJob(parenContext[Job])
        }
    }

    /**
     * Detach all plugins and terminate context
     */
    public open fun close() {
        @Suppress("DEPRECATION")
        if (isActive) error("Can't close active context")
        //detach all plugins
        plugins.forEach { it.detach() }
    }

    override fun toMeta(): Meta = Meta {
        "parent" to parent?.name
        "properties" put properties.layers.firstOrNull()
        "plugins" put plugins.map { it.toMeta() }
    }

    public companion object {
        public const val PROPERTY_TARGET: String = "context.property"
    }
}

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