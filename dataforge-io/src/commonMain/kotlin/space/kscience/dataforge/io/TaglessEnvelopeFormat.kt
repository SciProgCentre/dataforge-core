package space.kscience.dataforge.io

import io.ktor.utils.io.core.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import kotlin.collections.set

/**
 * A text envelope format with human-readable tag.
 * TODO add description
 */
public class TaglessEnvelopeFormat(
    public val io: IOPlugin,
    public val meta: Meta = Meta.EMPTY,
) : EnvelopeFormat {

    private val metaStart = meta[META_START_PROPERTY].string ?: DEFAULT_META_START
    private val dataStart = meta[DATA_START_PROPERTY].string ?: DEFAULT_DATA_START

    private fun Output.writeProperty(key: String, value: Any) {
        writeFully("#? $key: $value;\r\n".encodeToByteArray())
    }

    override fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory,
        formatMeta: Meta,
    ) {
        val metaFormat = metaFormatFactory.build(this.io.context, formatMeta)

        //printing header
        output.writeRawString(TAGLESS_ENVELOPE_HEADER + "\r\n")

        //printing all properties
        output.writeProperty(META_TYPE_PROPERTY,
            metaFormatFactory.shortName)
        //TODO add optional metaFormat properties
        val actualSize: Int = envelope.data?.size ?: 0

        output.writeProperty(DATA_LENGTH_PROPERTY, actualSize)

        //Printing meta
        if (!envelope.meta.isEmpty()) {
            val metaBinary = Binary(envelope.meta, metaFormat)
            output.writeProperty(META_LENGTH_PROPERTY,
                metaBinary.size + 2)
            output.writeUtf8String(this.metaStart + "\r\n")
            output.writeBinary(metaBinary)
            output.writeRawString("\r\n")
        }

        //Printing data
        envelope.data?.let { data ->
            output.writeUtf8String(this.dataStart + "\r\n")
            output.writeBinary(data)
        }
    }

    override fun readObject(input: Input): Envelope {
        //read preamble
        input.discardWithSeparator(
            TAGLESS_ENVELOPE_HEADER.encodeToByteArray(),
            atMost = 1024,
            skipUntilEndOfLine = true
        )

        val properties = HashMap<String, String>()

        var line = ""
        while (line.isBlank() || line.startsWith("#?")) {
            if (line.startsWith("#?")) {
                val match = propertyPattern.find(line)
                    ?: error("Line $line does not match property declaration pattern")
                val (key, value) = match.destructured
                properties[key] = value
            }
            try {
                line = ByteArray {
                    try {
                        input.readBytesWithSeparatorTo(this, byteArrayOf('\n'.code.toByte()), 1024)
                    } catch (ex: BufferLimitExceededException) {
                        throw IllegalStateException("Property line exceeds maximum line length (1024)", ex)
                    }
                }.decodeToString().trim()
            } catch (ex: EOFException) {
                return SimpleEnvelope(Meta.EMPTY, Binary.EMPTY)
            }
        }

        var meta: Meta = Meta.EMPTY

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.resolveMetaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties[META_LENGTH_PROPERTY]?.toInt()
            meta = if (metaSize != null) {
                metaFormat.readObjectFrom(input.readBinary(metaSize))
            } else {
                error("Can't partially read an envelope with undefined meta size")
            }
        }

        //skip until data start
        input.discardWithSeparator(
            dataStart.encodeToByteArray(),
            atMost = 1024,
            skipUntilEndOfLine = true
        )

        val data: Binary = if (properties.containsKey(DATA_LENGTH_PROPERTY)) {
            input.readBinary(properties[DATA_LENGTH_PROPERTY]!!.toInt())
//            val bytes = ByteArray(properties[DATA_LENGTH_PROPERTY]!!.toInt())
//            readByteArray(bytes)
//            bytes.asBinary()
        } else {
            input.readBytes().asBinary()
        }

        return SimpleEnvelope(meta, data)
    }


    override fun readPartial(input: Input): PartialEnvelope {
        var offset = 0

        //read preamble

        offset += input.discardWithSeparator(
            TAGLESS_ENVELOPE_HEADER.encodeToByteArray(),
            atMost = 1024,
            skipUntilEndOfLine = true
        )

        val properties = HashMap<String, String>()

        var line = ""
        while (line.isBlank() || line.startsWith("#?")) {
            if (line.startsWith("#?")) {
                val match = propertyPattern.find(line)
                    ?: error("Line $line does not match property declaration pattern")
                val (key, value) = match.destructured
                properties[key] = value
            }
            try {
                line = ByteArray {
                    val read = try {
                        input.readBytesWithSeparatorTo(this, byteArrayOf('\n'.code.toByte()), 1024)
                    } catch (ex: BufferLimitExceededException) {
                        throw IllegalStateException("Property line exceeds maximum line length (1024)", ex)
                    }
                    offset += read
                }.decodeToString().trim()
            } catch (ex: EOFException) {
                return PartialEnvelope(Meta.EMPTY, offset, 0.toULong())
            }
        }

        var meta: Meta = Meta.EMPTY

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.resolveMetaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties[META_LENGTH_PROPERTY]?.toInt()
            meta = if (metaSize != null) {
                offset += metaSize
                metaFormat.readObjectFrom(input.readBinary(metaSize))
            } else {
                error("Can't partially read an envelope with undefined meta size")
            }
        }

        //skip until data start
        offset += input.discardWithSeparator(
            dataStart.encodeToByteArray(),
            atMost = 1024,
            skipUntilEndOfLine = true
        )

        val dataSize = properties[DATA_LENGTH_PROPERTY]?.toULong()
        return PartialEnvelope(meta, offset, dataSize)
    }

    public companion object : EnvelopeFormatFactory {

        private val propertyPattern = "#\\?\\s*([\\w.]*)\\s*:\\s*([^;]*);?".toRegex()

        public const val META_TYPE_PROPERTY: String = "metaType"
        public const val META_LENGTH_PROPERTY: String = "metaLength"
        public const val DATA_LENGTH_PROPERTY: String = "dataLength"


        public const val TAGLESS_ENVELOPE_TYPE: String = "tagless"

        public const val TAGLESS_ENVELOPE_HEADER: String = "#~DFTL~#"
        public const val META_START_PROPERTY: String = "metaSeparator"
        public const val DEFAULT_META_START: String = "#~META~#"
        public const val DATA_START_PROPERTY: String = "dataSeparator"
        public const val DEFAULT_DATA_START: String = "#~DATA~#"

        public const val code: Int = 0x4446544c //DFTL

        override val name: Name = EnvelopeFormatFactory.ENVELOPE_FACTORY_NAME + TAGLESS_ENVELOPE_TYPE

        override fun build(context: Context, meta: Meta): EnvelopeFormat = TaglessEnvelopeFormat(context.io, meta)

        private val default by lazy { build(Global, Meta.EMPTY) }

        override fun readPartial(input: Input): PartialEnvelope =
            default.run { readPartial(input) }

        override fun writeEnvelope(
            output: Output,
            envelope: Envelope,
            metaFormatFactory: MetaFormatFactory,
            formatMeta: Meta,
        ): Unit = default.run {
            writeEnvelope(
                output,
                envelope,
                metaFormatFactory,
                formatMeta
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