package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.*
import hep.dataforge.names.asName
import kotlinx.io.core.*

class TaglessEnvelopeFormat(
    val io: IOPlugin,
    val metaType: String = JsonMetaFormat.name.toString(),
    meta: Meta = EmptyMeta
) : EnvelopeFormat {

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
            writeText(DEFAULT_META_START + "\r\n")
            writeFully(metaBytes)
            writeText("\r\n")
        }

        //Printing data
        obj.data?.let { data ->
            writeText(DEFAULT_DATA_START + "\r\n")
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

        if (line.startsWith(DEFAULT_META_START)) {
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
        } while (!line.startsWith(DEFAULT_DATA_START))

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    class TaglessReader(private val override: Map<String, String>) : EnvelopeReader {
//
//        private val BUFFER_SIZE = 1024
//
//        @Throws(IOException::class)
//        override fun read(stream: InputStream): Envelope {
//            return read(Channels.newChannel(stream))
//        }
//
//        override fun read(channel: ReadableByteChannel): Envelope {
//            val properties = HashMap(override)
//            val buffer = ByteBuffer.allocate(BUFFER_SIZE).apply { position(BUFFER_SIZE) }
//            val meta = readMeta(channel, buffer, properties)
//            return LazyEnvelope(meta) { BufferedBinary(readData(channel, buffer, properties)) }
//        }
//
//
//        /**
//         * Read lines using provided channel and buffer. Buffer state is changed by this operation
//         */
//        private fun readLines(channel: ReadableByteChannel, buffer: ByteBuffer): Sequence<String> {
//            return sequence {
//                val builder = ByteArrayOutputStream()
//                while (true) {
//                    if (!buffer.hasRemaining()) {
//                        if (!channel.isOpen) {
//                            break
//                        }
//                        buffer.flip()
//                        val count = channel.read(buffer)
//                        buffer.flip()
//                        if (count < BUFFER_SIZE) {
//                            channel.close()
//                        }
//                    }
//                    val b = buffer.get()
//                    builder.write(b.toInt())
//                    if (b == '\n'.toByte()) {
//                        yield(String(builder.toByteArray(), Charsets.UTF_8))
//                        builder.reset()
//                    }
//                }
//            }
//        }
//
//        @Throws(IOException::class)
//        private fun readMeta(
//            channel: ReadableByteChannel,
//            buffer: ByteBuffer,
//            properties: MutableMap<String, String>
//        ): Meta {
//            val sb = StringBuilder()
//            val metaEnd = properties.getOrDefault(DATA_START_PROPERTY, DEFAULT_DATA_START)
//            readLines(channel, buffer).takeWhile { it.trim { it <= ' ' } != metaEnd }.forEach { line ->
//                if (line.startsWith("#?")) {
//                    readProperty(line.trim(), properties)
//                } else if (line.isEmpty() || line.startsWith("#~")) {
//                    //Ignore headings, do nothing
//                } else {
//                    sb.append(line).append("\r\n")
//                }
//            }
//
//
//            return if (sb.isEmpty()) {
//                Meta.empty()
//            } else {
//                val metaType = MetaType.resolve(properties)
//                try {
//                    metaType.reader.readString(sb.toString())
//                } catch (e: ParseException) {
//                    throw RuntimeException("Failed to parse meta", e)
//                }
//
//            }
//        }
//
//
//        @Throws(IOException::class)
//        private fun readData(
//            channel: ReadableByteChannel,
//            buffer: ByteBuffer,
//            properties: Map<String, String>
//        ): ByteBuffer {
//            val array = ByteArray(buffer.remaining());
//            buffer.get(array)
//            if (properties.containsKey(DATA_LENGTH_PROPERTY)) {
//                val result = ByteBuffer.allocate(Integer.parseInt(properties[DATA_LENGTH_PROPERTY]))
//                result.put(array)//TODO fix it to not use direct array access
//                channel.read(result)
//                return result
//            } else {
//                val baos = ByteArrayOutputStream()
//                baos.write(array)
//                while (channel.isOpen) {
//                    val read = channel.read(buffer)
//                    buffer.flip()
//                    if (read < BUFFER_SIZE) {
//                        channel.close()
//                    }
//
//                    baos.write(buffer.array())
//                }
//                val remainingArray: ByteArray = ByteArray(buffer.remaining())
//                buffer.get(remainingArray)
//                baos.write(remainingArray)
//                return ByteBuffer.wrap(baos.toByteArray())
//            }
//        }
//    }

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