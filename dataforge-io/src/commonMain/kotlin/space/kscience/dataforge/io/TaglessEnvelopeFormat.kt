package space.kscience.dataforge.io


import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
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
        sink: Sink,
        obj: Envelope,
    ) {
        val metaFormat = metaFormatFactory.build(this.io.context, meta)

        //printing header
        sink.write(TAGLESS_ENVELOPE_HEADER)
        sink.writeString("\r\n")

        //Printing meta
        if (!obj.meta.isEmpty()) {
            val metaBinary = Binary(obj.meta, metaFormat)
            sink.writeString(META_START + "-${metaFormatFactory.shortName}\r\n")
            sink.writeBinary(metaBinary)
            sink.writeString("\r\n")
        }

        //Printing data
        obj.data?.let { data ->
            //val actualSize: Int = envelope.data?.size ?: 0
            sink.writeString(DATA_START + "\r\n")
            sink.writeBinary(data)
        }
    }

    override fun readObject(source: Source): Envelope {
        //read preamble
        source.discardWithSeparator(
            TAGLESS_ENVELOPE_HEADER,
            atMost = 1024,
        )

        var meta: Meta = Meta.EMPTY

        var data: Binary? = null

        source.discardWithSeparator(
            SEPARATOR_PREFIX,
            atMost = 1024,
        )

        var header: String = ByteArray {
            source.readWithSeparatorTo(this, "\n".encodeToByteString())
        }.decodeToString()

        while (!source.exhausted()) {
            val block = ByteArray {
                source.readWithSeparatorTo(this, SEPARATOR_PREFIX)
            }

            val nextHeader = ByteArray {
                source.readWithSeparatorTo(this, "\n".encodeToByteString())
            }.decodeToString()

            //terminate on end
            if (header.startsWith("END")) break


            if (header.startsWith("META")) {
                //TODO check format
                val metaFormat: MetaFormatFactory = JsonMetaFormat
                meta = metaFormat.readMeta(ByteArraySource(block).buffered())
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

        public val SEPARATOR_PREFIX: ByteString = "\n#~".encodeToByteString()

        public val TAGLESS_ENVELOPE_HEADER: ByteString = "#~DFTL".encodeToByteString()

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
            sink: Sink,
            obj: Envelope,
        ): Unit = default.run {
            writeObject(sink, obj)
        }

        override fun readObject(source: Source): Envelope = default.readObject(source)

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? {
            return try {
                binary.read {
                    val string = readByteString(TAGLESS_ENVELOPE_HEADER.size)
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