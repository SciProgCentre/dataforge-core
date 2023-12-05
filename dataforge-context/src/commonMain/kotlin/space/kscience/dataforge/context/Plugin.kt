package space.kscience.dataforge.context

import space.kscience.dataforge.context.Plugin.Companion.TARGET
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.misc.DfType
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.provider.Provider

/**
 * The interface to define a Context plugin. A plugin stores all runtime features of a context.
 * The plugin is by default configurable and a Provider (both features could be ignored).
 * The plugin must in most cases have an empty constructor in order to be able to load it from library.
 *
 * The plugin lifecycle is the following:
 *
 * create - configure - attach - detach - destroy
 */
@DfType(TARGET)
public interface Plugin : Named, ContextAware, Provider, MetaRepr {

    /**
     * Get tag for this plugin
     */
    public val tag: PluginTag

    public val meta: Meta

    /**
     * The name of this plugin ignoring version and group
     */
    override val name: Name get() = tag.name.parseAsName()

    /**
     * Plugin dependencies which are required to attach this plugin. Plugin
     * dependencies must be initialized and enabled in the Context before this
     * plugin is enabled.
     */
    public fun dependsOn(): Map<PluginFactory<*>, Meta>

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

    public val isAttached: Boolean

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