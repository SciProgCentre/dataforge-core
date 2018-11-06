package hep.dataforge.context

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.provideAll
import hep.dataforge.values.Value
import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KClass

interface Context : Named, MetaRepr, Provider {

    val parent: Context?

    /**
     * Context properties. Working as substitutes for environment variables
     */
    val properties: Meta

    /**
     * Context logger
     */
    val logger: KLogger

    val plugins: PluginManager

    /**
     * Defines if context is used in any kind of active computations. Active context properties and plugins could not be changed
     */
    val isActive: Boolean

    /**
     * Provide services for given type
     */
    fun <T : Any> services(type: KClass<T>): Sequence<T>

    override val defaultTarget: String get() = Plugin.PLUGIN_TARGET

    override fun provideTop(target: String, name: Name): Any? {
        return when (target) {
            Plugin.PLUGIN_TARGET -> plugins[PluginTag.fromString(name.toString())]
            Value.VALUE_TARGET -> properties[name]?.value
            else -> null
        }
    }

    override fun listTop(target: String): Sequence<Name> {
        return when (target) {
            Plugin.PLUGIN_TARGET -> plugins.asSequence().map { it.name.toName() }
            Value.VALUE_TARGET -> properties.asValueSequence().map { it.first }
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

    /**
     * Detach all plugins and terminate context
     */
    fun close()
}

/**
 * A sequences of all objects provided by plugins with given target and type
 */
inline fun <reified T : Any> Context.list(target: String): Sequence<T> {
    return plugins.asSequence().flatMap { provideAll(target) }.mapNotNull { it as? T }
}

/**
 * A global root context
 */
expect object Global : Context

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