package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.meta.configure
import kotlin.reflect.KClass

interface PluginFactory<T : Plugin> {
    val tag: PluginTag
    val type: KClass<out T>
    operator fun invoke(): T
}

operator fun PluginFactory<*>.invoke(meta: Meta) = invoke().configure(meta)


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
    PluginRepository.list().find { it.tag.matches(tag) }?.invoke(meta)
        ?: error("Plugin with tag $tag not found in the repository")

fun <T : Plugin> PluginRepository.register(
    tag: PluginTag,
    type: KClass<out T>,
    constructor: () -> T
): PluginFactory<T> {
    val factory = object : PluginFactory<T> {
        override val tag: PluginTag = tag
        override val type: KClass<out T> = type

        override fun invoke(): T = constructor()

    }
    PluginRepository.register(factory)
    return factory
}

inline fun <reified T : Plugin> PluginRepository.register(tag: PluginTag, noinline constructor: () -> T) =
    register(tag, T::class, constructor)

fun PluginRepository.register(plugin: Plugin) = register(plugin.tag, plugin::class) { plugin }