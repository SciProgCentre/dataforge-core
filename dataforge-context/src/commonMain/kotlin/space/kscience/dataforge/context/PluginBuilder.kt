package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name

/**
 * A convenience factory to build simple plugins
 */
public class PluginBuilder(
    name: String,
    group: String = "",
    version: String = "",
) {
    public val tag: PluginTag = PluginTag(name, group, version)

    private val content = HashMap<String, MutableMap<Name, Any>>()
    private val dependencies = HashMap<PluginFactory<*>, Meta>()

    public fun requires(
        factory: PluginFactory<*>,
        meta: Meta = Meta.EMPTY,
    ) {
        dependencies[factory] = meta
    }

    public fun provides(target: String, items: Map<Name, Any>) {
        content.getOrPut(target) { HashMap() }.putAll(items)
    }

    public fun provides(target: String, vararg items: Named) {
        provides(target, items.associateBy { it.name })
    }

    public fun build(): PluginFactory<*> {

        return object : PluginFactory<Plugin> {
            override val tag: PluginTag get() = this@PluginBuilder.tag

            override fun build(context: Context, meta: Meta): Plugin = object : AbstractPlugin() {
                override val tag: PluginTag get() = this@PluginBuilder.tag

                override fun content(target: String): Map<Name, Any> = this@PluginBuilder.content[target] ?: emptyMap()

                override fun dependsOn(): Map<PluginFactory<*>, Meta> = this@PluginBuilder.dependencies
            }

        }
    }
}

public fun PluginFactory(
    name: String,
    group: String = "",
    version: String = "",
    block: PluginBuilder.() -> Unit,
): PluginFactory<*> = PluginBuilder(name, group, version).apply(block).build()