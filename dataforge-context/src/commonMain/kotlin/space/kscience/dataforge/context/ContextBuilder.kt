package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.seal
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
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

    public fun properties(action: MutableMeta.() -> Unit) {
        meta.action()
    }

    public fun name(string: String) {
        this.name = Name.parse(string)
    }

    @OptIn(DFExperimental::class)
    private fun findPluginFactory(tag: PluginTag): PluginFactory<*> =
        parent.gatherInSequence<PluginFactory<*>>(PluginFactory.TYPE).values
            .find { it.tag.matches(tag) } ?: error("Can't resolve plugin factory for $tag")

    public fun plugin(tag: PluginTag, mutableMeta: MutableMeta.() -> Unit = {}) {
        val factory = findPluginFactory(tag)
        factories[factory] = Meta(mutableMeta)
    }

    public fun plugin(factory: PluginFactory<*>, meta: Meta) {
        factories[factory] = meta
    }

    public fun plugin(factory: PluginFactory<*>, mutableMeta: MutableMeta.() -> Unit = {}) {
        factories[factory] = Meta(mutableMeta)
    }

    public fun plugin(name: String, group: String = "", version: String = "", action: MutableMeta.() -> Unit = {}) {
        plugin(PluginTag(name, group, version), action)
    }

    /**
     * Add de-facto existing plugin as a dependency
     */
    public fun plugin(plugin: Plugin) {
        plugin(DeFactoPluginFactory(plugin))
    }

    public fun build(): Context {
        val contextName = name ?: NameToken("@auto",hashCode().toUInt().toString(16)).asName()
        val plugins = HashMap<PluginTag, Plugin>()

        fun addPlugin(factory: PluginFactory<*>, meta: Meta) {
            val existing = plugins[factory.tag]
            // Add if does not exist
            if (existing == null) {
                //TODO bypass if parent already has plugin with given meta?
                val plugin = factory(meta, parent)

                for ((depFactory, deoMeta) in plugin.dependsOn()) {
                    addPlugin(depFactory, deoMeta)
                }

                parent.logger.info { "Loading plugin ${plugin.name} into $contextName" }
                plugins[plugin.tag] = plugin
            } else if (existing.meta != meta) {
                error("Plugin with tag ${factory.tag} and meta $meta already exists in $contextName")
            }
            //bypass if exists with the same meta
        }

        factories.forEach { (factory, meta) ->
            addPlugin(factory, meta)
        }

        return Context(contextName, parent, plugins.values.toSet(), meta.seal())
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