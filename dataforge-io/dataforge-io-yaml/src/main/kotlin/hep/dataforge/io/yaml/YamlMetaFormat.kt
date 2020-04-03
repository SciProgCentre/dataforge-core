package hep.dataforge.io.yaml

import hep.dataforge.context.Context
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
import kotlinx.io.readUByte
import kotlinx.io.text.writeUtf8String
import org.yaml.snakeyaml.Yaml
import java.io.InputStream

@DFExperimental
class YamlMetaFormat(val meta: Meta) : MetaFormat {
    private val yaml = Yaml()

    override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) {
        val string = yaml.dump(meta.toMap(descriptor))
        writeUtf8String(string)
    }

    override fun Input.readMeta(descriptor: NodeDescriptor?): Meta {
        val map: Map<String, Any?> = yaml.load(asInputStream())
        return map.toMeta(descriptor)
    }

    companion object : MetaFormatFactory {
        override fun invoke(meta: Meta, context: Context): MetaFormat = YamlMetaFormat(meta)

        override val shortName = "yaml"

        override val key: Short = 0x594d //YM

        private val default = YamlMetaFormat()

        override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) =
            default.run { writeMeta(meta, descriptor) }

        override fun Input.readMeta(descriptor: NodeDescriptor?): Meta =
            default.run { readMeta(descriptor) }
    }
}