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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A convenience builder for context
 */
@DFBuilder
public class ContextBuilder internal constructor(
    private val parent: Context,
    public val name: Name? = null,
    meta: Meta = Meta.EMPTY,
) {
    internal val factories = HashMap<PluginFactory<*>, Meta>()
    internal var meta = meta.toMutableMeta()

    public fun properties(action: MutableMeta.() -> Unit) {
        meta.action()
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

    private var coroutineContext: CoroutineContext = EmptyCoroutineContext

    public fun coroutineContext(coroutineContext: CoroutineContext) {
        this.coroutineContext = coroutineContext
    }


    public fun build(): Context {
        val contextName = name ?: NameToken("@auto", hashCode().toUInt().toString(16)).asName()
        val plugins = HashMap<PluginTag, Plugin>()

        fun addPlugin(factory: PluginFactory<*>, meta: Meta) {
            val existing = plugins[factory.tag]
            // Add if it does not exist
            if (existing == null) {
                //TODO bypass if parent already has plugin with given meta?
                val plugin = factory.build(parent, meta)

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

        return Context(contextName, parent, plugins.values.toSet(), meta.seal(), coroutineContext)
    }
}