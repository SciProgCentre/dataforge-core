package hep.dataforge.io.yaml

import hep.dataforge.context.Context
import hep.dataforge.io.IOFormat.Companion.META_KEY
import hep.dataforge.io.IOFormat.Companion.NAME_KEY
import hep.dataforge.io.MetaFormat
import hep.dataforge.io.MetaFormatFactory
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.toMap
import hep.dataforge.meta.toMeta
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.asInputStream
import kotlinx.io.text.writeUtf8String
import org.yaml.snakeyaml.Yaml

/**
 * Represent meta as Yaml
 */
@DFExperimental
public class YamlMetaFormat(private val meta: Meta) : MetaFormat {
    private val yaml = Yaml()

    override fun writeMeta(output: Output, meta: Meta, descriptor: NodeDescriptor?) {
        val string = yaml.dump(meta.toMap(descriptor))
        output.writeUtf8String(string)
    }

    override fun readMeta(input: Input, descriptor: NodeDescriptor?): Meta {
        val map: Map<String, Any?> = yaml.load(input.asInputStream())
        return map.toMeta(descriptor)
    }

    override fun toMeta(): Meta  = Meta{
        NAME_KEY put FrontMatterEnvelopeFormat.name.toString()
        META_KEY put meta
    }

    public companion object : MetaFormatFactory {
        override fun invoke(meta: Meta, context: Context): MetaFormat = YamlMetaFormat(meta)

        override val shortName: String = "yaml"

        override val key: Short = 0x594d //YM

        private val default = YamlMetaFormat()

        override fun writeMeta(output: Output, meta: Meta, descriptor: NodeDescriptor?): Unit =
            default.writeMeta(output, meta, descriptor)

        override fun readMeta(input: kotlinx.io.Input, descriptor: NodeDescriptor?): Meta =
            default.readMeta(input, descriptor)
    }
}