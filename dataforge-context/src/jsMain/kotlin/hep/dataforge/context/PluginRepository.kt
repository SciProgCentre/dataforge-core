package hep.dataforge.context

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass


actual object PluginRepository {

    private val factories: MutableSet<PluginFactory> = HashSet()

    actual fun register(factory: PluginFactory) {
        factories.add(factory)
    }

    fun <T : Plugin> register(tag: PluginTag, type: KClass<out Plugin>, constructor: (Meta) -> T) {
        val factory = object : PluginFactory {
            override val tag: PluginTag = tag
            override val type: KClass<out Plugin> = type

            override fun build(meta: Meta): Plugin = constructor(meta)

        }
        register(factory)
    }

    inline fun <reified T : Plugin> register(tag: PluginTag, noinline constructor: (Meta) -> T) =
            register(tag, T::class, constructor)

    /**
     * List plugins available in the repository
     */
    actual fun list(): Sequence<PluginFactory> = factories.asSequence()
}