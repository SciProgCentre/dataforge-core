package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Type
import kotlin.reflect.KClass

@Type(PluginFactory.TYPE)
public interface PluginFactory<T : Plugin> : Factory<T> {
    public val tag: PluginTag
    public val type: KClass<out T>

    public companion object {
        public const val TYPE: String = "pluginFactory"
    }
}

/**
 * Plugin factory created for the specific actual plugin
 */
internal class DeFactoPluginFactory<T : Plugin>(val plugin: T) : PluginFactory<T> {
    override fun build(context: Context, meta: Meta): T = plugin
    override val tag: PluginTag get() = plugin.tag
    override val type: KClass<out T> get() = plugin::class
}
