package space.kscience.dataforge.io.yaml

import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.io.EnvelopeFormatFactory
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.MetaFormatFactory
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName

public class YamlPlugin(meta: Meta) : AbstractPlugin(meta) {
    public val io: IOPlugin by require(IOPlugin)

    override val tag: PluginTag get() = Companion.tag

    override fun content(target: String): Map<Name, Any> = when (target) {
        MetaFormatFactory.META_FORMAT_TYPE -> mapOf("yaml".asName() to YamlMetaFormat)
        EnvelopeFormatFactory.ENVELOPE_FORMAT_TYPE -> mapOf(FrontMatterEnvelopeFormat.name to FrontMatterEnvelopeFormat)
        else -> super.content(target)
    }

    public companion object : PluginFactory<YamlPlugin> {
        override val tag: PluginTag = PluginTag("io.yaml", group = PluginTag.DATAFORGE_GROUP)

        override fun build(context: Context, meta: Meta): YamlPlugin = YamlPlugin(meta)
    }
}