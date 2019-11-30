package hep.dataforge.io.yaml

import hep.dataforge.context.Context
import hep.dataforge.io.*
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.serialization.toUtf8Bytes

@DFExperimental
class FrontMatterEnvelopeFormat(
    val io: IOPlugin,
    meta: Meta = EmptyMeta
) : EnvelopeFormat {

    override fun Input.readPartial(): PartialEnvelope {
        var line: String = ""
        var offset = 0u
        do {
            line = readUtf8Line() ?: error("Input does not contain front matter separator")
            offset += line.toUtf8Bytes().size.toUInt()
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.metaFormat(it) } ?: YamlMetaFormat

        val metaBlock = buildPacket {
            do {
                line = readUtf8Line() ?: error("Input does not contain closing front matter separator")
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
            line = readUtf8Line() ?: error("Input does not contain front matter separator")
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.metaFormat(it) } ?: YamlMetaFormat

        val metaBlock = buildPacket {
            do {
                appendln(readUtf8Line() ?: error("Input does not contain closing front matter separator"))
            } while (!line.startsWith(SEPARATOR))
        }
        val meta = readMetaFormat.fromBytes(metaBlock)
        val bytes = readBytes()
        val data = bytes.asBinary()
        return SimpleEnvelope(meta, data)
    }

    override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) {
        val metaFormat = metaFormatFactory(formatMeta, io.context)
        writeText("$SEPARATOR\r\n")
        metaFormat.run { writeObject(envelope.meta) }
        writeText("$SEPARATOR\r\n")
        envelope.data?.read { copyTo(this@writeEnvelope) }
    }

    companion object : EnvelopeFormatFactory {
        const val SEPARATOR = "---"

        private val metaTypeRegex = "---(\\w*)\\s*".toRegex()

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            return FrontMatterEnvelopeFormat(context.io, meta)
        }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            val line = input.readUtf8Line(3, 30)
            return if (line != null && line.startsWith("---")) {
                invoke()
            } else {
                null
            }
        }

        private val default by lazy { invoke() }

        override fun Input.readPartial(): PartialEnvelope =
            default.run { readPartial() }

        override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) =
            default.run { writeEnvelope(envelope, metaFormatFactory, formatMeta) }

        override fun Input.readObject(): Envelope =
            default.run { readObject() }

    }
}