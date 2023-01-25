package space.kscience.dataforge.io.yaml

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
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

    override fun readObject(binary: Binary): Envelope = binary.read {
        var offset = 0

        offset += discardWithSeparator(
            SEPARATOR.encodeToByteArray(),
            atMost = 1024,
        )

        val line = ByteArray {
            offset += readWithSeparatorTo(this, "\n".encodeToByteArray())
        }.decodeToString()

        val readMetaFormat = line.trim().takeIf { it.isNotBlank() }?.let { io.resolveMetaFormat(it) } ?: YamlMetaFormat

        val packet = ByteArray {
            offset += readWithSeparatorTo(this, SEPARATOR.encodeToByteArray())
        }

        offset += discardLine()

        val meta = readMetaFormat.readObject(packet.asBinary())
        Envelope(meta, binary.view(offset))
    }

    override fun readObject(input: Input): Envelope = readObject(input.readBinary())

    override fun writeObject(
        output: Output,
        obj: Envelope,
    ) {
        val metaFormat = metaFormatFactory.build(io.context, meta)
        val formatSuffix = if (metaFormat is YamlMetaFormat) "" else metaFormatFactory.shortName
        output.writeRawString("$SEPARATOR${formatSuffix}\r\n")
        metaFormat.run { metaFormat.writeObject(output, obj.meta) }
        output.writeRawString("$SEPARATOR\r\n")
        //Printing data
        obj.data?.let { data ->
            output.writeBinary(data)
        }
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

        override fun readObject(binary: Binary): Envelope = default.readObject(binary)

        override fun writeObject(
            output: Output,
            obj: Envelope,
        ): Unit = default.writeObject(output, obj)


        override fun readObject(input: Input): Envelope = default.readObject(input)

    }
}