package space.kscience.dataforge.io.yaml

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.readByteString
import kotlinx.io.writeString
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus

public class FrontMatterEnvelopeFormat(
    private val io: IOPlugin,
    private val meta: Meta = Meta.EMPTY,
    private val metaFormatFactory: MetaFormatFactory = YamlMetaFormat,
) : EnvelopeFormat {

    override fun readFrom(binary: Binary): Envelope = binary.read {
        var offset = 0

        offset += discardWithSeparator(
            SEPARATOR,
            atMost = 1024,
        )

        val line = ByteArray {
            offset += readWithSeparatorTo(this, "\n".encodeToByteString())
        }.decodeToString()

        val readMetaFormat = line.trim().takeIf { it.isNotBlank() }?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        val packet = ByteArray {
            offset += readWithSeparatorTo(this, SEPARATOR)
        }

        offset += discardLine()

        val meta = readMetaFormat.readFrom(packet.asBinary())
        Envelope(meta, binary.view(offset))
    }

    override fun readFrom(source: Source): Envelope = readFrom(source.readBinary())

    override fun writeTo(
        sink: Sink,
        obj: Envelope,
    ) {
        val metaFormat = metaFormatFactory.build(io.context, meta)
        val formatSuffix = if (metaFormat is YamlMetaFormat) "" else metaFormatFactory.shortName
        sink.writeString("$SEPARATOR${formatSuffix}\r\n")
        metaFormat.run { metaFormat.writeTo(sink, obj.meta) }
        sink.writeString("$SEPARATOR\r\n")
        //Printing data
        obj.data?.let { data ->
            sink.writeBinary(data)
        }
    }

    public companion object : EnvelopeFormatFactory {
        public val SEPARATOR: ByteString = "---".encodeToByteString()

        private val metaTypeRegex = "---(\\w*)\\s*".toRegex()

        override val name: Name = EnvelopeFormatFactory.ENVELOPE_FACTORY_NAME + "frontMatter"

        override fun build(context: Context, meta: Meta): EnvelopeFormat {
            return FrontMatterEnvelopeFormat(context.io, meta)
        }

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? = binary.read {
            //read raw string to avoid UTF issues
            val line = readByteString(3)
            return@read if (line == "---".encodeToByteString()) {
                default
            } else {
                null
            }
        }

        private val default by lazy { build(Global, Meta.EMPTY) }

        override fun readFrom(binary: Binary): Envelope = default.readFrom(binary)

        override fun writeTo(
            sink: Sink,
            obj: Envelope,
        ): Unit = default.writeTo(sink, obj)


        override fun readFrom(source: Source): Envelope = default.readFrom(source)

    }
}