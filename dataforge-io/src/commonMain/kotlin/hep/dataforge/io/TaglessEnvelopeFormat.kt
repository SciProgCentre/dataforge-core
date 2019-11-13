package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.*
import hep.dataforge.names.asName
import kotlinx.io.core.*
import kotlinx.serialization.toUtf8Bytes

class TaglessEnvelopeFormat(
    val io: IOPlugin,
    meta: Meta = EmptyMeta
) : EnvelopeFormat {

    private val metaStart = meta[META_START_PROPERTY].string ?: DEFAULT_META_START
    private val dataStart = meta[DATA_START_PROPERTY].string ?: DEFAULT_DATA_START

    private fun Output.writeProperty(key: String, value: Any) {
        writeText("#? $key: $value;\r\n")
    }

    override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) {
        val metaFormat = metaFormatFactory(formatMeta, io.context)

        //printing header
        writeText(TAGLESS_ENVELOPE_HEADER + "\r\n")

        //printing all properties
        writeProperty(META_TYPE_PROPERTY, metaFormatFactory.type)
        //TODO add optional metaFormat properties
        val actualSize: ULong = if (envelope.data == null) {
            0u
        } else {
            envelope.data?.size ?: ULong.MAX_VALUE
        }

        writeProperty(DATA_LENGTH_PROPERTY, actualSize)

        //Printing meta
        if (!envelope.meta.isEmpty()) {
            val metaBytes = metaFormat.writePacket(envelope.meta)
            writeProperty(META_LENGTH_PROPERTY, metaBytes.remaining)
            writeText(metaStart + "\r\n")
            writePacket(metaBytes)
            writeText("\r\n")
        }

        //Printing data
        envelope.data?.let { data ->
            writeText(dataStart + "\r\n")
            writeFully(data.toBytes())
        }
        flush()
    }

    override fun Input.readObject(): Envelope {
        var line: String = ""
        do {
            line = readUTF8Line() ?: error("Input does not contain tagless envelope header")
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
            line = readUTF8Line() ?: return SimpleEnvelope(Meta.empty, null)
        }

        var meta: Meta = EmptyMeta

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.metaFormat(it) } ?: JsonMetaFormat
            val metaSize = properties.get(META_LENGTH_PROPERTY)?.toInt()
            meta = if (metaSize != null) {
                val metaPacket = buildPacket {
                    writeFully(readBytes(metaSize))
                }
                metaFormat.run { metaPacket.readObject() }
            } else {
                metaFormat.run {
                    readObject()
                }
            }
        }

        do {
            line = readUTF8Line() ?: return SimpleEnvelope(meta, null)
            //returning an Envelope without data if end of input is reached
        } while (!line.startsWith(dataStart))

        val data: Binary? = if (properties.containsKey(DATA_LENGTH_PROPERTY)) {
            val bytes = ByteArray(properties[DATA_LENGTH_PROPERTY]!!.toInt())
            readFully(bytes)
            bytes.asBinary()
        } else {
            val bytes = readBytes()
            bytes.asBinary()
        }

        return SimpleEnvelope(meta, data)
    }

    override fun Input.readPartial(): PartialEnvelope {
        var offset = 0u
        var line: String = ""
        do {
            line = readUTF8Line() ?: error("Input does not contain tagless envelope header")
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
            line = readUTF8Line() ?: return PartialEnvelope(Meta.empty, offset.toUInt(), 0.toULong())
            offset += line.toUtf8Bytes().size.toUInt()
        }

        var meta: Meta = EmptyMeta

        if (line.startsWith(metaStart)) {
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.metaFormat(it) } ?: JsonMetaFormat

            val metaSize = properties.get(META_LENGTH_PROPERTY)?.toInt()
            meta = if (metaSize != null) {
                val metaPacket = buildPacket {
                    writeFully(readBytes(metaSize))
                }
                offset += metaSize.toUInt()
                metaFormat.run { metaPacket.readObject() }
            } else {
                error("Can't partially read an envelope with undefined meta size")
            }
        }

        do {
            line = readUTF8Line() ?: return PartialEnvelope(Meta.empty, offset.toUInt(), 0.toULong())
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
                val buffer = ByteArray(TAGLESS_ENVELOPE_HEADER.length)
                input.readFully(buffer)
                return if (String(buffer) == TAGLESS_ENVELOPE_HEADER) {
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