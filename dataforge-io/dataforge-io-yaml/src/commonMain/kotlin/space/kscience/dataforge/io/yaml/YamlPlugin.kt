package space.kscience.dataforge.io.yaml

import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.MetaFormatFactory
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.reflect.KClass

@DFExperimental
public class YamlPlugin(meta: Meta) : AbstractPlugin(meta) {
    public val io: IOPlugin by require(IOPlugin)

    override val tag: PluginTag get() = Companion.tag

    override fun content(target: String): Map<Name, Any> = when (target) {
        MetaFormatFactory.META_FORMAT_TYPE -> mapOf("yaml".asName() to YamlMetaFormat)
        else -> super.content(target)
    }

    public companion object : PluginFactory<YamlPlugin> {
        override val tag: PluginTag = PluginTag("io.yaml", group = PluginTag.DATAFORGE_GROUP)

        override val type: KClass<out YamlPlugin> = YamlPlugin::class
        override fun invoke(meta: Meta, context: Context): YamlPlugin = YamlPlugin(meta)
    }
}