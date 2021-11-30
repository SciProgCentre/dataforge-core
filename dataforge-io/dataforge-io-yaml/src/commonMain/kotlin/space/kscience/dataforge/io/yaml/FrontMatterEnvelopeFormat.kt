package space.kscience.dataforge.io.yaml

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.readUTF8Line
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.*
import space.kscience.dataforge.io.IOFormat.Companion.META_KEY
import space.kscience.dataforge.io.IOFormat.Companion.NAME_KEY
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental

@DFExperimental
public class FrontMatterEnvelopeFormat(
    private val io: IOPlugin,
    private val meta: Meta = Meta.EMPTY,
) : EnvelopeFormat {

    override fun readPartial(input: Input): PartialEnvelope {
        var line: String
        var offset = 0u
        do {
            line = input.readUTF8Line() ?: error("Input does not contain front matter separator")
            offset += line.encodeToByteArray().size.toUInt()
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        //TODO replace by preview
        val meta = Binary {
            do {
                line = input.readSafeUtf8Line()
                writeUtf8String(line + "\r\n")
                offset += line.encodeToByteArray().size.toUInt()
            } while (!line.startsWith(SEPARATOR))
        }.read {
            readMetaFormat.readMeta(input)

        }
        return PartialEnvelope(meta, offset, null)
    }

    override fun readObject(input: Input): Envelope {
        var line: String
        do {
            line = input.readSafeUtf8Line() //?: error("Input does not contain front matter separator")
        } while (!line.startsWith(SEPARATOR))

        val readMetaFormat =
            metaTypeRegex.matchEntire(line)?.groupValues?.first()
                ?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        val meta = Binary {
            do {
                writeUtf8String(input.readSafeUtf8Line() + "\r\n")
            } while (!line.startsWith(SEPARATOR))
        }.read {
            readMetaFormat.readMeta(input)
        }
        val bytes = input.readBytes()
        val data = bytes.asBinary()
        return SimpleEnvelope(meta, data)
    }

    override fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory,
        formatMeta: Meta,
    ) {
        val metaFormat = metaFormatFactory(formatMeta, this@FrontMatterEnvelopeFormat.io.context)
        output.writeRawString("$SEPARATOR\r\n")
        metaFormat.run { this.writeObject(output, envelope.meta) }
        output.writeRawString("$SEPARATOR\r\n")
        //Printing data
        envelope.data?.let { data ->
            output.writeBinary(data)
        }
    }

    override fun toMeta(): Meta = Meta {
        NAME_KEY put name.toString()
        META_KEY put meta
    }

    public companion object : EnvelopeFormatFactory {
        public const val SEPARATOR: String = "---"

        private val metaTypeRegex = "---(\\w*)\\s*".toRegex()

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            return FrontMatterEnvelopeFormat(context.io, meta)
        }

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? = binary.read {
            val line = readSafeUtf8Line()
            return@read if (line.startsWith("---")) {
                invoke()
            } else {
                null
            }
        }

        private val default by lazy { invoke() }

        override fun readPartial(input: Input): PartialEnvelope =
            default.readPartial(input)

        override fun writeEnvelope(
            output: Output,
            envelope: Envelope,
            metaFormatFactory: MetaFormatFactory,
            formatMeta: Meta,
        ): Unit = FrontMatterEnvelopeFormat.default.writeEnvelope(output, envelope, metaFormatFactory, formatMeta)


        override fun readObject(input: Input): Envelope = default.readObject(input)

    }
}