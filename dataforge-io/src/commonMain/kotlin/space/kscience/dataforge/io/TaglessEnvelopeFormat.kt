package space.kscience.dataforge.io

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import io.ktor.utils.io.core.readUTF8UntilDelimiterTo
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus

/**
 * A text envelope format based on block separators.
 * TODO add description
 */
public class TaglessEnvelopeFormat(
    public val io: IOPlugin,
    public val meta: Meta = Meta.EMPTY,
    public val metaFormatFactory: MetaFormatFactory = JsonMetaFormat,
) : EnvelopeFormat {

//    private val metaStart = meta[META_START_PROPERTY].string ?: DEFAULT_META_START
//    private val dataStart = meta[DATA_START_PROPERTY].string ?: DEFAULT_DATA_START

//    private fun Output.writeProperty(key: String, value: Any) {
//        writeFully("#? $key: $value;\r\n".encodeToByteArray())
//    }

    override fun writeObject(
        output: Output,
        obj: Envelope,
    ) {
        val metaFormat = metaFormatFactory.build(this.io.context, meta)

        //printing header
        output.writeRawString(TAGLESS_ENVELOPE_HEADER + "\r\n")

        //Printing meta
        if (!obj.meta.isEmpty()) {
            val metaBinary = Binary(obj.meta, metaFormat)
            output.writeUtf8String(META_START + "-${metaFormatFactory.shortName}\r\n")
            output.writeBinary(metaBinary)
            output.writeRawString("\r\n")
        }

        //Printing data
        obj.data?.let { data ->
            //val actualSize: Int = envelope.data?.size ?: 0
            output.writeUtf8String(DATA_START + "\r\n")
            output.writeBinary(data)
        }
    }

    override fun readObject(input: Input): Envelope {
        //read preamble
        input.discardWithSeparator(
            TAGLESS_ENVELOPE_HEADER.encodeToByteArray(),
            atMost = 1024,
        )

        var meta: Meta = Meta.EMPTY

        var data: Binary? = null

        input.discardWithSeparator(
            SEPARATOR_PREFIX,
            atMost = 1024,
        )

        var header: String = ByteArray {
            input.readUTF8UntilDelimiterTo(this, "\n")
        }.decodeToString()

        while (!input.endOfInput) {
            val block = ByteArray {
                input.readWithSeparatorTo(this, SEPARATOR_PREFIX)
            }

            val nextHeader = ByteArray {
                input.readWithSeparatorTo(this, "\n".encodeToByteArray())
            }.decodeToString()

            //terminate on end
            if (header.startsWith("END")) break


            if (header.startsWith("META")) {
                //TODO check format
                val metaFormat: MetaFormatFactory = JsonMetaFormat
                meta = metaFormat.readMeta(ByteReadPacket(block))
            }

            if (header.startsWith("DATA")) {
                data = block.asBinary()
            }
            header = nextHeader
        }
        return Envelope(meta, data)
    }

    public companion object : EnvelopeFormatFactory {

        private val propertyPattern = "#\\?\\s*([\\w.]*)\\s*:\\s*([^;]*);?".toRegex()

        public const val META_TYPE_PROPERTY: String = "metaType"
        public const val META_LENGTH_PROPERTY: String = "metaLength"
        public const val DATA_LENGTH_PROPERTY: String = "dataLength"


        public const val TAGLESS_ENVELOPE_TYPE: String = "tagless"

        public val SEPARATOR_PREFIX: ByteArray = "\n#~".encodeToByteArray()

        public const val TAGLESS_ENVELOPE_HEADER: String = "#~DFTL"

        //        public const val META_START_PROPERTY: String = "metaSeparator"
        public const val META_START: String = "#~META"

        //        public const val DATA_START_PROPERTY: String = "dataSeparator"
        public const val DATA_START: String = "#~DATA"

        public const val END: String = "#~END"

        public const val code: Int = 0x4446544c //DFTL

        override val name: Name = EnvelopeFormatFactory.ENVELOPE_FACTORY_NAME + TAGLESS_ENVELOPE_TYPE

        override fun build(context: Context, meta: Meta): EnvelopeFormat = TaglessEnvelopeFormat(context.io, meta)

        private val default by lazy { build(Global, Meta.EMPTY) }

        override fun readObject(binary: Binary): Envelope = default.run { readObject(binary) }

        override fun writeObject(
            output: Output,
            obj: Envelope,
        ): Unit = default.run {
            writeObject(
                output,
                obj,
            )
        }

        override fun readObject(input: Input): Envelope = default.readObject(input)

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? {
            return try {
                binary.read {
                    val string = readRawString(TAGLESS_ENVELOPE_HEADER.length)
                    return@read if (string == TAGLESS_ENVELOPE_HEADER) {
                        TaglessEnvelopeFormat(io)
                    } else {
                        null
                    }
                }
            } catch (ex: Exception) {
                null
            }
        }
    }
}