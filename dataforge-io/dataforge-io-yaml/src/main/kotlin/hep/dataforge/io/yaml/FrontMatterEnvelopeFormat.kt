package hep.dataforge.io.yaml

import hep.dataforge.context.Context
import hep.dataforge.io.*
import hep.dataforge.io.IOFormat.Companion.META_KEY
import hep.dataforge.io.IOFormat.Companion.NAME_KEY
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import kotlinx.io.*
import kotlinx.io.text.readUtf8Line
import kotlinx.io.text.writeUtf8String

@DFExperimental
public class FrontMatterEnvelopeFormat(
    private val io: IOPlugin,
    private val meta: Meta = Meta.EMPTY,
) : EnvelopeFormat {

    override fun readPartial(input: Input): PartialEnvelope {
        var line = ""
        var offset = 0u
        do {
            line = input.readUtf8Line() //?: error("Input does not contain front matter separator")
            offset += line.toByteArray().size.toUInt()
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        //TODO replace by preview
        val meta = Binary {
            do {
                line = input.readUtf8Line()
                writeUtf8String(line + "\r\n")
                offset += line.toByteArray().size.toUInt()
            } while (!line.startsWith(SEPARATOR))
        }.read {
            readMetaFormat.readMeta(input)

        }
        return PartialEnvelope(meta, offset, null)
    }

    override fun readObject(input: Input): Envelope {
        var line = ""
        do {
            line = input.readUtf8Line() //?: error("Input does not contain front matter separator")
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        val meta = Binary {
            do {
                writeUtf8String(input.readUtf8Line() + "\r\n")
            } while (!line.startsWith(SEPARATOR))
        }.read {
            readMetaFormat.readMeta(input)
        }
        val bytes = input.readByteArray()
        val data = bytes.asBinary()
        return SimpleEnvelope(meta, data)
    }

    override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) {
        val metaFormat = metaFormatFactory(formatMeta, io.context)
        writeRawString("$SEPARATOR\r\n")
        metaFormat.run { writeObject(this@writeEnvelope, envelope.meta) }
        writeRawString("$SEPARATOR\r\n")
        //Printing data
        envelope.data?.let { data ->
            writeBinary(data)
        }
    }

    override fun toMeta(): Meta = Meta {
        NAME_KEY put name.toString()
        META_KEY put meta
    }

    public companion object : EnvelopeFormatFactory {
        public const val SEPARATOR = "---"

        private val metaTypeRegex = "---(\\w*)\\s*".toRegex()

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            return FrontMatterEnvelopeFormat(context.io, meta)
        }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return input.preview {
                val line = readUtf8Line()
                return@preview if (line.startsWith("---")) {
                    invoke()
                } else {
                    null
                }
            }
        }

        private val default by lazy { invoke() }

        override fun readPartial(input: Input): PartialEnvelope =
            default.readPartial(input)

        override fun Output.writeEnvelope(
            envelope: Envelope,
            metaFormatFactory: MetaFormatFactory,
            formatMeta: Meta,
        ): Unit = default.run { writeEnvelope(envelope, metaFormatFactory, formatMeta) }

        override fun readObject(input: Input): Envelope = default.readObject(input)

    }
}