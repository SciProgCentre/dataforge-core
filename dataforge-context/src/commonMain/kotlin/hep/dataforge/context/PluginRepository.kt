package hep.dataforge.context

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

interface PluginFactory {
    val tag: PluginTag
    val type: KClass<out Plugin>
    fun build(meta: Meta): Plugin
}


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
fun PluginRepository.fetch(tag: PluginTag, meta: Meta): Plugin =
    PluginRepository.list().find { it.tag.matches(tag) }?.build(meta)
        ?: error("Plugin with tag $tag not found in the repository")