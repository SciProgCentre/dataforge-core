package hep.dataforge.context

import hep.dataforge.meta.Config
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.configure

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

    fun plugin(tag: PluginTag, action: Config.() -> Unit) {
        plugins.add(PluginRepository.fetch(tag).configure(action))
    }

    fun plugin(name: String, group: String = "", version: String = "", action: Config.() -> Unit) {
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