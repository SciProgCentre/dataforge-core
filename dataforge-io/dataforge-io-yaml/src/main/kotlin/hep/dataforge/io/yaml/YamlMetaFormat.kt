package hep.dataforge.io.yaml

import hep.dataforge.context.Context
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.io.MetaFormat
import hep.dataforge.io.MetaFormatFactory
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.meta.toMap
import hep.dataforge.meta.toMeta
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.core.readUByte
import kotlinx.io.core.writeText
import org.yaml.snakeyaml.Yaml
import java.io.InputStream

private class InputAsStream(val input: Input) : InputStream() {
    override fun read(): Int {
        if (input.endOfInput) return -1
        return input.readUByte().toInt()
    }

    override fun close() {
        input.close()
    }
}

private fun Input.asStream() = InputAsStream(this)

@DFExperimental
class YamlMetaFormat(val meta: Meta) : MetaFormat {
    private val yaml = Yaml()

    override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) {
        val string = yaml.dump(meta.toMap(descriptor))
        writeText(string)
    }

    override fun Input.readMeta(descriptor: NodeDescriptor?): Meta {
        val map: Map<String, Any?> = yaml.load(asStream())
        return map.toMeta(descriptor)
    }

    companion object : MetaFormatFactory {
        override fun invoke(meta: Meta, context: Context): MetaFormat = YamlMetaFormat(meta)

        override val name: Name = super.name + "yaml"

        override val key: Short = 0x594d //YM

        private val default = YamlMetaFormat()

        override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) =
            default.run { writeMeta(meta, descriptor) }

        override fun Input.readMeta(descriptor: NodeDescriptor?): Meta =
            default.run { readMeta(descriptor) }
    }
}