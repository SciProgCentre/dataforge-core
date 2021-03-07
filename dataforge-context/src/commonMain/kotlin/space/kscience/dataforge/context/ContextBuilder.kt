package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaBuilder
import space.kscience.dataforge.meta.seal
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.toName
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * A convenience builder for context
 */
@DFBuilder
public class ContextBuilder(private val parent: Context = Global, public var name: String = "@anonymous") {
    private val factories = HashMap<PluginFactory<*>, Meta>()
    private var meta = MetaBuilder()

    public fun properties(action: MetaBuilder.() -> Unit) {
        meta.action()
    }

    @OptIn(DFExperimental::class)
    private fun findPluginFactory(tag: PluginTag): PluginFactory<*> =
        parent.gatherInSequence<PluginFactory<*>>(PluginFactory.TYPE).values
            .find { it.tag.matches(tag) } ?: error("Can't resolve plugin factory for $tag")

    public fun plugin(tag: PluginTag, metaBuilder: MetaBuilder.() -> Unit = {}) {
        val factory = findPluginFactory(tag)
        factories[factory] = Meta(metaBuilder)
    }

    public fun plugin(factory: PluginFactory<*>, metaBuilder: MetaBuilder.() -> Unit = {}) {
        factories[factory] = Meta(metaBuilder)
    }

    public fun plugin(name: String, group: String = "", version: String = "", action: MetaBuilder.() -> Unit = {}) {
        plugin(PluginTag(name, group, version), action)
    }

    public fun build(): Context {
        return Context(name.toName(), parent, meta.seal()).apply {
            factories.forEach { (factory, meta) ->
                plugins.load(factory, meta)
            }
        }
    }
}