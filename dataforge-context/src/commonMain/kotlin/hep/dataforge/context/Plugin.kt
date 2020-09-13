package hep.dataforge.context

import hep.dataforge.context.Plugin.Companion.TARGET
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.Type

/**
 * The interface to define a Context plugin. A plugin stores all runtime features of a context.
 * The plugin is by default configurable and a Provider (both features could be ignored).
 * The plugin must in most cases have an empty constructor in order to be able to load it from library.
 *
 * The plugin lifecycle is the following:
 *
 * create - configure - attach - detach - destroy
 */
@Type(TARGET)
public interface Plugin : Named, ContextAware, Provider, MetaRepr {

    /**
     * Get tag for this plugin
     */
    public val tag: PluginTag

    public val meta: Meta

    /**
     * The name of this plugin ignoring version and group
     */
    override val name: Name get() = tag.name.toName()

    /**
     * Plugin dependencies which are required to attach this plugin. Plugin
     * dependencies must be initialized and enabled in the Context before this
     * plugin is enabled.
     */
    public fun dependsOn(): Collection<PluginFactory<*>>

    /**
     * Start this plugin and attach registration info to the context. This method
     * should be called only via PluginManager to avoid dependency issues.
     */
    public fun attach(context: Context)

    /**
     * Stop this plugin and remove registration info from context and other
     * plugins. This method should be called only via PluginManager to avoid
     * dependency issues.
     */
    public fun detach()

    override fun toMeta(): Meta = Meta {
        "context" put context.name.toString()
        "type" to this::class.simpleName
        "tag" put tag
        "meta" put meta
    }

    public companion object {
        public const val TARGET: String = "plugin"
    }

}