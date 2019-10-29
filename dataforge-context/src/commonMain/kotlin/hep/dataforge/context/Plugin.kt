package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider

/**
 * The interface to define a Context plugin. A plugin stores all runtime features of a context.
 * The plugin is by default configurable and a Provider (both features could be ignored).
 * The plugin must in most cases have an empty constructor in order to be able to load it from library.
 *
 *
 * The plugin lifecycle is the following:
 *
 *
 * create - configure - attach - detach - destroy
 *
 *
 * Configuration of attached plugin is possible for a context which is not in a runtime mode, but it is not recommended.
 *
 * @author Alexander Nozik
 */
interface Plugin : Named, ContextAware, Provider, MetaRepr {

    /**
     * Get tag for this plugin
     *
     * @return
     */
    val tag: PluginTag

    val meta: Meta

    /**
     * The name of this plugin ignoring version and group
     *
     * @return
     */
    override val name: Name get() = tag.name.toName()

    /**
     * Plugin dependencies which are required to attach this plugin. Plugin
     * dependencies must be initialized and enabled in the Context before this
     * plugin is enabled.
     *
     * @return
     */
    fun dependsOn(): Collection<PluginFactory<*>>

    /**
     * Start this plugin and attach registration info to the context. This method
     * should be called only via PluginManager to avoid dependency issues.
     *
     * @param context
     */
    fun attach(context: Context)

    /**
     * Stop this plugin and remove registration info from context and other
     * plugins. This method should be called only via PluginManager to avoid
     * dependency issues.
     */
    fun detach()

    override fun toMeta(): Meta = buildMeta {
        "context" put context.name.toString()
        "type" to this::class.simpleName
        "tag" put tag
        "meta" put meta
    }

    companion object {

        const val PLUGIN_TARGET = "plugin"
    }

}