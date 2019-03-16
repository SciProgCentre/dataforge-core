package hep.dataforge.context

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.provideAll
import hep.dataforge.values.Value
import kotlinx.coroutines.CoroutineScope
import mu.KLogger
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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
open class Context(final override val name: String, val parent: Context? = Global) : Named, MetaRepr, Provider,
    CoroutineScope {

    private val config = Config()

    /**
     * Context properties. Working as substitute for environment variables
     */
    val properties: Meta = if (parent == null) {
        config
    } else {
        Laminate(config, parent.properties)
    }

    /**
     * Context logger
     */
    val logger: KLogger = KotlinLogging.logger(name)

    /**
     * A [PluginManager] for current context
     */
    val plugins: PluginManager by lazy { PluginManager(this) }

    private val activators = HashSet<Any>()

    /**
     * Defines if context is used in any kind of active computations. Active context properties and plugins could not be changed
     */
    val isActive: Boolean = activators.isNotEmpty()

    override val defaultTarget: String get() = Plugin.PLUGIN_TARGET

    override fun provideTop(target: String, name: Name): Any? {
        return when (target) {
            Plugin.PLUGIN_TARGET -> plugins[PluginTag.fromString(name.toString())]
            Value.TYPE -> properties[name]?.value
            else -> null
        }
    }

    override fun listTop(target: String): Sequence<Name> {
        return when (target) {
            Plugin.PLUGIN_TARGET -> plugins.asSequence().map { it.name.toName() }
            Value.TYPE -> properties.asValueSequence().map { it.first }
            else -> emptySequence()
        }
    }

    /**
     * Mark context as active and used by [activator]
     */
    fun activate(activator: Any) {
        activators.add(activator)
    }

    /**
     * Mark context unused by [activator]
     */
    fun deactivate(activator: Any) {
        activators.remove(activator)
    }

    /**
     * Change the properties of the context. If active, throw an exception
     */
    fun configure(action: Config.() -> Unit) {
        if (isActive) error("Can't configure active context")
        config.action()
    }

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    /**
     * Detach all plugins and terminate context
     */
    open fun close() {
        if (isActive) error("Can't close active context")
        //detach all plugins
        plugins.forEach { it.detach() }
    }

    override fun toMeta(): Meta = buildMeta {
        "parent" to parent?.name
        "properties" to properties.seal()
        "plugins" to plugins.map { it.toMeta() }
    }
}

/**
 * A sequences of all objects provided by plugins with given target and type
 */
fun Context.members(target: String): Sequence<Any> =
    plugins.asSequence().flatMap { it.provideAll(target) }

@JvmName("typedMembers")
inline fun <reified T : Any> Context.members(target: String) =
    members(target).filterIsInstance<T>()


/**
 * A global root context. Closing [Global] terminates the framework.
 */
object Global : Context("GLOBAL", null) {
    /**
     * Closing all contexts
     *
     * @throws Exception
     */
    override fun close() {
        logger.info { "Shutting down GLOBAL" }
        for (ctx in contextRegistry.values) {
            ctx.close()
        }
        super.close()
    }

    private val contextRegistry = HashMap<String, Context>()

    /**
     * Get previously builder context o builder a new one
     *
     * @param name
     * @return
     */
    fun getContext(name: String): Context {
        return contextRegistry.getOrPut(name) { Context(name) }
    }
}


/**
 * The interface for something that encapsulated in context
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface ContextAware {
    /**
     * Get context for this object
     *
     * @return
     */
    val context: Context

    val logger: KLogger
        get() = if (this is Named) {
            KotlinLogging.logger(context.name + "." + (this as Named).name)
        } else {
            context.logger
        }

}