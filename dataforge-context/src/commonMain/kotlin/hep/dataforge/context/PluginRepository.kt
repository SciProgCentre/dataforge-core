package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

interface PluginFactory<T : Plugin> : Factory<T> {
    val tag: PluginTag
    val type: KClass<out T>
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
    list().find { it.tag.matches(tag) }?.invoke(meta = meta)
        ?: error("Plugin with tag $tag not found in the repository")

fun <T : Plugin> PluginRepository.register(
    tag: PluginTag,
    type: KClass<out T>,
    constructor: (Context, Meta) -> T
): PluginFactory<T> {
    val factory = object : PluginFactory<T> {
        override val tag: PluginTag = tag
        override val type: KClass<out T> = type

        override fun invoke(meta: Meta, context: Context): T = constructor(context, meta)

    }
    register(factory)
    return factory
}

inline fun <reified T : Plugin> PluginRepository.register(tag: PluginTag, noinline constructor: (Context, Meta) -> T) =
    register(tag, T::class, constructor)

fun PluginRepository.register(plugin: Plugin) = register(plugin.tag, plugin::class) { _, _ -> plugin }