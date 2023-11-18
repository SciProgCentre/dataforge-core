package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DfId

@DfId(PluginFactory.TYPE)
public interface PluginFactory<T : Plugin> : Factory<T> {
    public val tag: PluginTag

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
}
