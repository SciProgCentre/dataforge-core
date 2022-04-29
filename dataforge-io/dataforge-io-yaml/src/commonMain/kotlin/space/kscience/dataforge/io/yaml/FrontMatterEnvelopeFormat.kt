package space.kscience.dataforge.io.yaml

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readBytes
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.io.*
import space.kscience.dataforge.io.IOFormat.Companion.META_KEY
import space.kscience.dataforge.io.IOFormat.Companion.NAME_KEY
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus

@DFExperimental
public class FrontMatterEnvelopeFormat(
    private val io: IOPlugin,
    private val meta: Meta = Meta.EMPTY,
) : EnvelopeFormat {

    override fun readPartial(input: Input): PartialEnvelope {
        var offset = 0

        offset += input.discardWithSeparator(
            SEPARATOR.encodeToByteArray(),
            atMost = 1024,
            skipUntilEndOfLine = false
        )

        val line = input.readSafeUtf8Line()
        val readMetaFormat = line.trim().takeIf { it.isNotBlank() }?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        //TODO replace by preview
        val packet =  buildPacket {
            offset += input.readBytesWithSeparatorTo(
                this,
                SEPARATOR.encodeToByteArray(),
                skipUntilEndOfLine = true
            )
        }
        val meta = readMetaFormat.readMeta(packet)
        return PartialEnvelope(meta, offset, null)
    }

    override fun readObject(input: Input): Envelope {
        val partial = readPartial(input)
        val data = input.readBytes().asBinary()
        return SimpleEnvelope(partial.meta, data)
    }

    override fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory,
        formatMeta: Meta,
    ) {
        val metaFormat = metaFormatFactory.build(this@FrontMatterEnvelopeFormat.io.context, formatMeta)
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

        override val name: Name = EnvelopeFormatFactory.ENVELOPE_FACTORY_NAME + "frontMatter"

        override fun build(context: Context, meta: Meta): EnvelopeFormat {
            return FrontMatterEnvelopeFormat(context.io, meta)
        }

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? = binary.read {
            val line = readSafeUtf8Line()
            return@read if (line.startsWith("---")) {
                default
            } else {
                null
            }
        }

        private val default by lazy { build(Global, Meta.EMPTY) }

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