package hep.dataforge.context

import hep.dataforge.meta.*
import hep.dataforge.names.toName

/**
 * A convenience builder for context
 */
@DFBuilder
public class ContextBuilder(private val parent: Context = Global, public var name: String = "@anonymous") {
    private val plugins = ArrayList<Plugin>()
    private var meta = MetaBuilder()

    public fun properties(action: MetaBuilder.() -> Unit) {
        meta.action()
    }

    public fun plugin(plugin: Plugin) {
        plugins.add(plugin)
    }

    @OptIn(DFExperimental::class)
    public fun plugin(tag: PluginTag, metaBuilder: MetaBuilder.() -> Unit = {}) {
        val factory = parent.gatherInSequence<PluginFactory<*>>(PluginFactory.TYPE).values
            .find { it.tag.matches(tag) } ?: error("Can't resolve plugin factory for $tag")
        val plugin = factory.invoke(Meta(metaBuilder), parent)
        plugins.add(plugin)
    }

    public fun plugin(builder: PluginFactory<*>, action: MetaBuilder.() -> Unit = {}) {
        plugins.add(builder.invoke(Meta(action)))
    }

    public fun plugin(name: String, group: String = "", version: String = "", action: MetaBuilder.() -> Unit = {}) {
        plugin(PluginTag(name, group, version), action)
    }

    public fun build(): Context {
        return Context(name.toName(), parent, meta.seal()).apply {
            this@ContextBuilder.plugins.forEach {
                plugins.load(it)
            }
        }
    }
}