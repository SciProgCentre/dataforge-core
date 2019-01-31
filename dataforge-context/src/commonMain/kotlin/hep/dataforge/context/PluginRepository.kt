package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.meta.configure
import kotlin.reflect.KClass

interface PluginFactory {
    val tag: PluginTag
    val type: KClass<out Plugin>
    fun build(): Plugin
}

fun PluginFactory.build(meta: Meta) = build().configure(meta)


expect object PluginRepository {

    fun register(factory: PluginFactory)

    /**
     * List plugins available in the repository
     */
    fun list(): Sequence<PluginFactory>

}

/**
 * Fetch specific plugin and instantiate it with given meta
 */
fun PluginRepository.fetch(tag: PluginTag): Plugin =
    PluginRepository.list().find { it.tag.matches(tag) }?.build()
        ?: error("Plugin with tag $tag not found in the repository")

fun PluginRepository.register(tag: PluginTag, type: KClass<out Plugin>, constructor: () -> Plugin) {
    val factory = object : PluginFactory {
        override val tag: PluginTag = tag
        override val type: KClass<out Plugin> = type

        override fun build(): Plugin = constructor()

    }
    PluginRepository.register(factory)
}

inline fun <reified T : Plugin> PluginRepository.register(tag: PluginTag, noinline constructor: () -> T) =
    register(tag, T::class, constructor)

fun PluginRepository.register(plugin: Plugin) = register(plugin.tag, plugin::class) { plugin }