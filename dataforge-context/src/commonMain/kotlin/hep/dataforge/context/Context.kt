package hep.dataforge.context

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.values.Value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * Since plugins could contain mutable state, context has two states: active and inactive. No changes are allowed to active context.
 * @author Alexander Nozik
 */
interface Context : Named, MetaRepr, Provider, CoroutineScope {

    val parent: Context?

    /**
     * Context properties. Working as substitute for environment variables
     */
    val properties: Meta

    /**
     * Context logger
     */
    val logger: KLogger

    /**
     * A [PluginManager] for current context
     */
    val plugins: PluginManager

    /**
     * Defines if context is used in any kind of active computations. Active context properties and plugins could not be changed
     */
    val isActive: Boolean

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
    fun activate(activator: Any)

    /**
     * Mark context unused by [activator]
     */
    fun deactivate(activator: Any)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /**
     * Detach all plugins and terminate context
     */
    fun close()

    override fun toMeta(): Meta = buildMeta {
        "parent" to parent?.name
        "properties" to properties.seal()
        "plugins" to plugins.map { it.toMeta() }
    }
}


/**
 * A global root context. Closing [Global] terminates the framework.
 */
expect object Global : Context {
    fun getContext(name: String): Context
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