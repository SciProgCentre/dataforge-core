package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.*
import hep.dataforge.names.asName
import kotlinx.io.*
import kotlinx.io.text.readRawString
import kotlinx.io.text.readUtf8Line
import kotlinx.io.text.writeRawString
import kotlinx.io.text.writeUtf8String
import kotlinx.serialization.toUtf8Bytes

@ExperimentalIoApi
class TaglessEnvelopeFormat(
    val io: IOPlugin,
    meta: Meta = EmptyMeta
) : EnvelopeFormat {

    private val metaStart = meta[META_START_PROPERTY].string ?: DEFAULT_META_START
    private val dataStart = meta[DATA_START_PROPERTY].string ?: DEFAULT_DATA_START

    private fun Output.writeProperty(key: String, value: Any) {
        writeUtf8String("#? $key: $value;\r\n")
    }

    override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) {
        val metaFormat = metaFormatFactory(formatMeta, io.context)

        //printing header
        writeRawString(TAGLESS_ENVELOPE_HEADER + "\r\n")

        //printing all properties
        writeProperty(META_TYPE_PROPERTY, metaFormatFactory.shortName)
        //TODO add optional metaFormat properties
        val actualSize: Int = if (envelope.data == null) {
            0
        } else {
            envelope.data?.size ?: Binary.INFINITE
        }

        writeProperty(DATA_LENGTH_PROPERTY, actualSize)

        //Printing meta
        if (!envelope.meta.isEmpty()) {
            val metaBytes = metaFormat.writeBytes(envelope.meta)
            writeProperty(META_LENGTH_PROPERTY, metaBytes.size + 2)
            writeUtf8String(metaStart + "\r\n")
            writeBinary(metaBytes)
            writeRawString("\r\n")
        }

        //Printing data
        envelope.data?.let { data ->
            writeUtf8String(dataStart + "\r\n")
            writeBinary(data)
        }
    }

    override fun Input.readObject(): Envelope {
        var line: String
        do {
            line = readUtf8Line() // ?: error("Input does not contain tagless envelope header")
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
            if (eof()) return SimpleEnvelope(Meta.EMPTY, null)
            line = readUtf8Line()
        }

        var meta: Meta = EmptyMeta

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.metaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties[META_LENGTH_PROPERTY]?.toInt()
            meta = if (metaSize != null) {
                limit(metaSize).run {
                    metaFormat.run { readObject() }
                }
            } else {
                metaFormat.run {
                    readObject()
                }
            }
        }

        do {
            try {
                line = readUtf8Line()
            } catch (ex: EOFException) {
                //returning an Envelope without data if end of input is reached
                return SimpleEnvelope(meta, null)
            }
        } while (!line.startsWith(dataStart))

        val data: Binary? = if (properties.containsKey(DATA_LENGTH_PROPERTY)) {
            val bytes = ByteArray(properties[DATA_LENGTH_PROPERTY]!!.toInt())
            readArray(bytes)
            bytes.asBinary()
        } else {
            ArrayBinary.write {
                writeInput(this@readObject)
            }
        }

        return SimpleEnvelope(meta, data)
    }

    override fun Input.readPartial(): PartialEnvelope {
        var offset = 0u
        var line: String
        do {
            line = readUtf8Line()// ?: error("Input does not contain tagless envelope header")
            offset += line.toUtf8Bytes().size.toUInt()
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
                line = readUtf8Line()
                offset += line.toUtf8Bytes().size.toUInt()
            } catch (ex: EOFException) {
                return PartialEnvelope(Meta.EMPTY, offset.toUInt(), 0.toULong())
            }
        }

        var meta: Meta = EmptyMeta

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.metaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties[META_LENGTH_PROPERTY]?.toInt()
            meta = if (metaSize != null) {
                offset += metaSize.toUInt()
                limit(metaSize).run {
                    metaFormat.run { readObject() }
                }
            } else {
                error("Can't partially read an envelope with undefined meta size")
            }
        }

        do {
            line = readUtf8Line() ?: return PartialEnvelope(Meta.EMPTY, offset.toUInt(), 0.toULong())
            offset += line.toUtf8Bytes().size.toUInt()
            //returning an Envelope without data if end of input is reached
        } while (!line.startsWith(dataStart))

        val dataSize = properties[DATA_LENGTH_PROPERTY]?.toULong()
        return PartialEnvelope(meta, offset, dataSize)
    }

    companion object : EnvelopeFormatFactory {

        private val propertyPattern = "#\\?\\s*(?<key>[\\w.]*)\\s*:\\s*(?<value>[^;]*);?".toRegex()

        const val META_TYPE_PROPERTY = "metaType"
        const val META_LENGTH_PROPERTY = "metaLength"
        const val DATA_LENGTH_PROPERTY = "dataLength"


        const val TAGLESS_ENVELOPE_TYPE = "tagless"

        const val TAGLESS_ENVELOPE_HEADER = "#~DFTL~#"
        const val META_START_PROPERTY = "metaSeparator"
        const val DEFAULT_META_START = "#~META~#"
        const val DATA_START_PROPERTY = "dataSeparator"
        const val DEFAULT_DATA_START = "#~DATA~#"

        const val code: Int = 0x4446544c //DFTL

        override val name = TAGLESS_ENVELOPE_TYPE.asName()

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            return TaglessEnvelopeFormat(context.io, meta)
        }

        private val default by lazy { invoke() }

        override fun Input.readPartial(): PartialEnvelope =
            default.run { readPartial() }

        override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) =
            default.run { writeEnvelope(envelope, metaFormatFactory, formatMeta) }

        override fun Input.readObject(): Envelope =
            default.run { readObject() }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                val string = input.readRawString(TAGLESS_ENVELOPE_HEADER.length)
                return if (string == TAGLESS_ENVELOPE_HEADER) {
                    TaglessEnvelopeFormat(io)
                } else {
                    null
                }
            } catch (ex: Exception) {
                null
            }
        }
    }
}