package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaBuilder
import space.kscience.dataforge.meta.seal
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.names.toName
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * A convenience builder for context
 */
@DFBuilder
public class ContextBuilder internal constructor(
    private val parent: Context,
    public var name: Name? = null,
    meta: Meta = Meta.EMPTY,
) {
    internal val factories = HashMap<PluginFactory<*>, Meta>()
    internal var meta = meta.toMutableMeta()

    public fun properties(action: MetaBuilder.() -> Unit) {
        meta.action()
    }

    public fun name(string: String) {
        this.name = string.toName()
    }

    @OptIn(DFExperimental::class)
    private fun findPluginFactory(tag: PluginTag): PluginFactory<*> =
        parent.gatherInSequence<PluginFactory<*>>(PluginFactory.TYPE).values
            .find { it.tag.matches(tag) } ?: error("Can't resolve plugin factory for $tag")

    public fun plugin(tag: PluginTag, metaBuilder: MetaBuilder.() -> Unit = {}) {
        val factory = findPluginFactory(tag)
        factories[factory] = Meta(metaBuilder)
    }

    public fun plugin(factory: PluginFactory<*>, meta: Meta) {
        factories[factory] = meta
    }

    public fun plugin(factory: PluginFactory<*>, metaBuilder: MetaBuilder.() -> Unit = {}) {
        factories[factory] = Meta(metaBuilder)
    }

    public fun plugin(name: String, group: String = "", version: String = "", action: MetaBuilder.() -> Unit = {}) {
        plugin(PluginTag(name, group, version), action)
    }

    /**
     * Add de-facto existing plugin as a dependency
     */
    public fun plugin(plugin: Plugin) {
        plugin(DeFactoPluginFactory(plugin))
    }

    public fun build(): Context {
        val contextName = name ?: "@auto[${hashCode().toUInt().toString(16)}]".toName()
        return Context(contextName, parent, meta.seal()).apply {
            factories.forEach { (factory, meta) ->
                plugins.load(factory, meta)
            }
        }
    }
}

/**
 * Check if current context contains all plugins required by the builder and return it it does or forks to a new context
 * if it does not.
 */
public fun Context.withEnv(block: ContextBuilder.() -> Unit): Context {

    fun Context.contains(factory: PluginFactory<*>, meta: Meta): Boolean {
        val loaded = plugins[factory.tag] ?: return false
        return loaded.meta == meta
    }

    val builder = ContextBuilder(this, name + "env", properties).apply(block)
    val requiresFork = builder.factories.any { (factory, meta) ->
        !contains(factory, meta)
    } || ((properties as Meta) == builder.meta)
    return if (requiresFork) builder.build() else this
}