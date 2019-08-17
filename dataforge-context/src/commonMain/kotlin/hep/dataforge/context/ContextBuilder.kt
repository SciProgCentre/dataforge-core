package hep.dataforge.context

import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta

/**
 * A convenience builder for context
 */
class ContextBuilder(var name: String = "@anonimous", val parent: Context = Global) {
    private val plugins = ArrayList<Plugin>()
    private var meta = MetaBuilder()

    fun properties(action: MetaBuilder.() -> Unit) {
        meta.action()
    }

    fun plugin(plugin: Plugin) {
        plugins.add(plugin)
    }

    fun plugin(tag: PluginTag, action: MetaBuilder.() -> Unit = {}) {
        plugins.add(PluginRepository.fetch(tag, buildMeta(action)))
    }

    fun plugin(builder: PluginFactory<*>, action: MetaBuilder.() -> Unit = {}) {
        plugins.add(builder.invoke(buildMeta(action)))
    }

    fun plugin(name: String, group: String = "", version: String = "", action: MetaBuilder.() -> Unit = {}) {
        plugin(PluginTag(name, group, version), action)
    }

    fun build(): Context {
        return Context(name, parent).apply {
            this@ContextBuilder.plugins.forEach {
                plugins.load(it)
            }
        }
    }
}