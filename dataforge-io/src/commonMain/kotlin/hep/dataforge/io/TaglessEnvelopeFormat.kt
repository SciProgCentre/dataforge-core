package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.IOFormat.Companion.META_KEY
import hep.dataforge.io.IOFormat.Companion.NAME_KEY
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.isEmpty
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import kotlinx.io.*
import kotlinx.io.text.readUtf8Line
import kotlinx.io.text.writeUtf8String
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
        writeUtf8String("#? $key: $value;\r\n")
    }

    override fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory,
        formatMeta: Meta
    ) {
        val metaFormat = metaFormatFactory(formatMeta, this.io.context)

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
            val metaBytes = metaFormat.toBinary(envelope.meta)
            output.writeProperty(META_LENGTH_PROPERTY,
                metaBytes.size + 2)
            output.writeUtf8String(this.metaStart + "\r\n")
            output.writeBinary(metaBytes)
            output.writeRawString("\r\n")
        }

        //Printing data
        envelope.data?.let { data ->
            output.writeUtf8String(this.dataStart + "\r\n")
            output.writeBinary(data)
        }
    }

    override fun readObject(input: Input): Envelope {
        var line: String
        do {
            line = input.readUtf8Line() // ?: error("Input does not contain tagless envelope header")
        } while (!line.startsWith(TAGLESS_ENVELOPE_HEADER))
        val properties = HashMap<String, String>()

        line = ""
        while (line.isBlank() || line.startsWith("#?")) {
            if (line.startsWith("#?")) {
                val match = propertyPattern.find(line)
                    ?: error("Line $line does not match property declaration pattern")
                val (key, value) = match.destructured
                properties[key] = value
            }
            //If can't read line, return envelope without data
            if (input.exhausted()) return SimpleEnvelope(Meta.EMPTY, null)
            line = input.readUtf8Line()
        }

        var meta: Meta = Meta.EMPTY

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.resolveMetaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties[META_LENGTH_PROPERTY]?.toInt()
            meta = if (metaSize != null) {
                metaFormat.readObject(input.limit(metaSize))
            } else {
                metaFormat.readObject(input)
            }
        }

        do {
            try {
                line = input.readUtf8Line()
            } catch (ex: EOFException) {
                //returning an Envelope without data if end of input is reached
                return SimpleEnvelope(meta, null)
            }
        } while (!line.startsWith(dataStart))

        val data: Binary? = if (properties.containsKey(DATA_LENGTH_PROPERTY)) {
            input.readBinary(properties[DATA_LENGTH_PROPERTY]!!.toInt())
//            val bytes = ByteArray(properties[DATA_LENGTH_PROPERTY]!!.toInt())
//            readByteArray(bytes)
//            bytes.asBinary()
        } else {
            Binary {
                input.copyTo(this)
            }
        }

        return SimpleEnvelope(meta, data)
    }

    override fun readPartial(input: Input): PartialEnvelope {
        var offset = 0u
        var line: String
        do {
            line = input.readUtf8Line()// ?: error("Input does not contain tagless envelope header")
            offset += line.encodeToByteArray().size.toUInt()
        } while (!line.startsWith(TAGLESS_ENVELOPE_HEADER))
        val properties = HashMap<String, String>()

        line = ""
        while (line.isBlank() || line.startsWith("#?")) {
            if (line.startsWith("#?")) {
                val match = propertyPattern.find(line)
                    ?: error("Line $line does not match property declaration pattern")
                val (key, value) = match.destructured
                properties[key] = value
            }
            try {
                line = input.readUtf8Line()
                offset += line.encodeToByteArray().size.toUInt()
            } catch (ex: EOFException) {
                return PartialEnvelope(Meta.EMPTY, offset.toUInt(), 0.toULong())
            }
        }

        var meta: Meta = Meta.EMPTY

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.resolveMetaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties[META_LENGTH_PROPERTY]?.toInt()
            meta = if (metaSize != null) {
                offset += metaSize.toUInt()
                metaFormat.readObject(input.limit(metaSize))
            } else {
                error("Can't partially read an envelope with undefined meta size")
            }
        }

        do {
            line = input.readUtf8Line() //?: return PartialEnvelope(Meta.EMPTY, offset.toUInt(), 0.toULong())
            offset += line.encodeToByteArray().size.toUInt()
            //returning an Envelope without data if end of input is reached
        } while (!line.startsWith(dataStart))

        val dataSize = properties[DATA_LENGTH_PROPERTY]?.toULong()
        return PartialEnvelope(meta, offset, dataSize)
    }

    override fun toMeta(): Meta = Meta {
        NAME_KEY put name.toString()
        META_KEY put meta
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

        override val name: Name = TAGLESS_ENVELOPE_TYPE.asName()

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            return TaglessEnvelopeFormat(context.io, meta)
        }

        private val default by lazy { invoke() }

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

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                input.preview {
                    val string = readRawString(TAGLESS_ENVELOPE_HEADER.length)
                    return@preview if (string == TAGLESS_ENVELOPE_HEADER) {
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