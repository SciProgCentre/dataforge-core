package hep.dataforge.context

import hep.dataforge.meta.DFBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.toName

/**
 * A convenience builder for context
 */
@DFBuilder
class ContextBuilder(var name: String = "@anonymous", val parent: Context = Global) {
    private val plugins = ArrayList<Plugin>()
    private var meta = MetaBuilder()

    fun properties(action: MetaBuilder.() -> Unit) {
        meta.action()
    }

    fun plugin(plugin: Plugin) {
        plugins.add(plugin)
    }

    fun plugin(tag: PluginTag, action: MetaBuilder.() -> Unit = {}) {
        plugins.add(PluginRepository.fetch(tag, Meta(action)))
    }

    fun plugin(builder: PluginFactory<*>, action: MetaBuilder.() -> Unit = {}) {
        plugins.add(builder.invoke(Meta(action)))
    }

    fun plugin(name: String, group: String = "", version: String = "", action: MetaBuilder.() -> Unit = {}) {
        plugin(PluginTag(name, group, version), action)
    }

    fun build(): Context {
        return Context(name.toName(), parent).apply {
            this@ContextBuilder.plugins.forEach {
                plugins.load(it)
            }
        }
    }
}