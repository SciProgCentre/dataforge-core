package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.*
import hep.dataforge.names.asName
import kotlinx.io.core.*
import kotlinx.serialization.toUtf8Bytes

class TaglessEnvelopeFormat(
    val io: IOPlugin,
    val metaType: String = JsonMetaFormat.name.toString(),
    meta: Meta = EmptyMeta
) : EnvelopeFormat {

    private val metaStart = meta[META_START_PROPERTY].string ?: DEFAULT_META_START
    private val dataStart = meta[DATA_START_PROPERTY].string ?: DEFAULT_DATA_START

    val metaFormat = io.metaFormat(metaType, meta)
        ?: error("Meta format with type $metaType could not be resolved in $io")

    private fun Output.writeProperty(key: String, value: Any) {
        writeText("#? $key: $value;\r\n")
    }

    override fun Output.writeThis(obj: Envelope) {

        //printing header
        writeText(TAGLESS_ENVELOPE_HEADER + "\r\n")

        //printing all properties
        writeProperty(META_TYPE_PROPERTY, metaType)
        //TODO add optional metaFormat properties
        writeProperty(DATA_LENGTH_PROPERTY, obj.data?.size ?: 0)

        //Printing meta
        if (!obj.meta.isEmpty()) {
            val metaBytes = metaFormat.writeBytes(obj.meta)
            writeProperty(META_LENGTH_PROPERTY, metaBytes.size)
            writeText(metaStart + "\r\n")
            writeFully(metaBytes)
            writeText("\r\n")
        }

        //Printing data
        obj.data?.let { data ->
            writeText(dataStart + "\r\n")
            writeFully(data.toBytes())
        }
    }

    override fun Input.readThis(): Envelope {
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
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.metaFormat(it) } ?: JsonMetaFormat.default
            meta = if (properties.containsKey(META_LENGTH_PROPERTY)) {
                val bytes = ByteArray(properties[META_LENGTH_PROPERTY]!!.toInt())
                readFully(bytes)
                metaFormat.readBytes(bytes)
            } else {
                metaFormat.run {
                    readThis()
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
            val metaFormat = properties[META_TYPE_PROPERTY]?.let { io.metaFormat(it) } ?: JsonMetaFormat.default
            meta = if (properties.containsKey(META_LENGTH_PROPERTY)) {
                val bytes = ByteArray(properties[META_LENGTH_PROPERTY]!!.toInt())
                readFully(bytes)
                offset += bytes.size.toUInt()
                metaFormat.readBytes(bytes)
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
            val metaFormatName: String = meta["name"].string ?: JsonMetaFormat.name.toString()
            return TaglessEnvelopeFormat(context.io, metaFormatName, meta)
        }

        val default by lazy { invoke() }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                val buffer = ByteArray(TAGLESS_ENVELOPE_HEADER.length)
                input.readFully(buffer)
                return if (buffer.toString() == TAGLESS_ENVELOPE_HEADER) {
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