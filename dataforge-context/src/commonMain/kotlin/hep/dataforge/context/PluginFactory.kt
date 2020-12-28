package hep.dataforge.context

import hep.dataforge.provider.Type
import kotlin.reflect.KClass

@Type(PluginFactory.TYPE)
public interface PluginFactory<T : Plugin> : Factory<T> {
    public val tag: PluginTag
    public val type: KClass<out T>

    public companion object {
        public const val TYPE: String = "pluginFactory"
    }
}
