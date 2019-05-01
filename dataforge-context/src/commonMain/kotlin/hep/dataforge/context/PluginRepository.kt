package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

interface PluginFactory<T : Plugin> {
    val tag: PluginTag
    val type: KClass<out T>
    operator fun invoke(meta: Meta = EmptyMeta): T
}

expect object PluginRepository {

    fun register(factory: PluginFactory<*>)

    /**
     * List plugins available in the repository
     */
    fun list(): Sequence<PluginFactory<*>>

}

/**
 * Fetch specific plugin and instantiate it with given meta
 */
fun PluginRepository.fetch(tag: PluginTag, meta: Meta = EmptyMeta): Plugin =
    list().find { it.tag.matches(tag) }?.invoke(meta) ?: error("Plugin with tag $tag not found in the repository")

fun <T : Plugin> PluginRepository.register(
    tag: PluginTag,
    type: KClass<out T>,
    constructor: (Meta) -> T
): PluginFactory<T> {
    val factory = object : PluginFactory<T> {
        override val tag: PluginTag = tag
        override val type: KClass<out T> = type

        override fun invoke(meta: Meta): T = constructor(meta)

    }
    register(factory)
    return factory
}

inline fun <reified T : Plugin> PluginRepository.register(tag: PluginTag, noinline constructor: (Meta) -> T) =
    register(tag, T::class, constructor)

fun PluginRepository.register(plugin: Plugin) = register(plugin.tag, plugin::class) { plugin }