package hep.dataforge.io.yaml

import hep.dataforge.context.Context
import hep.dataforge.io.*
import hep.dataforge.meta.*
import kotlinx.io.core.*
import kotlinx.serialization.toUtf8Bytes

@DFExperimental
class FrontMatterEnvelopeFormat(
    val io: IOPlugin,
    val metaType: String = YamlMetaFormat.name.toString(),
    meta: Meta = EmptyMeta
) : EnvelopeFormat {

    val metaFormat = io.metaFormat(metaType, meta)
        ?: error("Meta format with type $metaType could not be resolved in $io")

    override fun Input.readPartial(): PartialEnvelope {
        var line: String = ""
        var offset = 0u
        do {
            line = readUTF8Line() ?: error("Input does not contain front matter separator")
            offset += line.toUtf8Bytes().size.toUInt()
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.metaFormat(it) } ?: YamlMetaFormat.default

        val metaBlock = buildPacket {
            do {
                line = readUTF8Line() ?: error("Input does not contain closing front matter separator")
                appendln(line)
                offset += line.toUtf8Bytes().size.toUInt()
            } while (!line.startsWith(SEPARATOR))
        }
        val meta = readMetaFormat.fromBytes(metaBlock)
        return PartialEnvelope(meta, offset, null)
    }

    override fun Input.readObject(): Envelope {
        var line: String = ""
        do {
            line = readUTF8Line() ?: error("Input does not contain front matter separator")
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.metaFormat(it) } ?: YamlMetaFormat.default

        val metaBlock = buildPacket {
            do {
                appendln(readUTF8Line() ?: error("Input does not contain closing front matter separator"))
            } while (!line.startsWith(SEPARATOR))
        }
        val meta = readMetaFormat.fromBytes(metaBlock)
        val bytes = readBytes()
        val data = bytes.asBinary()
        return SimpleEnvelope(meta, data)
    }

    override fun Output.writeObject(obj: Envelope) {
        writeText("$SEPARATOR\r\n")
        metaFormat.run { writeObject(obj.meta) }
        writeText("$SEPARATOR\r\n")
        obj.data?.read { copyTo(this@writeObject) }
    }

    companion object : EnvelopeFormatFactory {
        const val SEPARATOR = "---"

        private val metaTypeRegex = "---(\\w*)\\s*".toRegex()

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            val metaFormatName: String = meta["name"].string ?: YamlMetaFormat.name.toString()
            return FrontMatterEnvelopeFormat(context.io, metaFormatName, meta)
        }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            val line = input.readUTF8Line(3, 30)
            return if (line != null && line.startsWith("---")) {
                invoke()
            } else {
                null
            }
        }

    }
}