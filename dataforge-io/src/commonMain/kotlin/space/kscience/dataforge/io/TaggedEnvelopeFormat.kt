package space.kscience.dataforge.io

import kotlinx.io.*
import kotlinx.io.bytestring.decodeToString
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.enum
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus


/**
 * A streaming-friendly envelope format with a short binary tag.
 * TODO add description
 */
public class TaggedEnvelopeFormat(
    public val io: IOPlugin,
    public val version: VERSION = VERSION.DF02,
    public val metaFormatFactory: MetaFormatFactory = JsonMetaFormat,
) : EnvelopeFormat {

//    private val metaFormat = io.metaFormat(metaFormatKey)
//        ?: error("Meta format with key $metaFormatKey could not be resolved in $io")


    private fun Tag.toBinary() = Binary {
        write(START_SEQUENCE)
        writeString(version.name)
        writeShort(metaFormatKey)
        writeUInt(metaSize)
        when (version) {
            VERSION.DF02 -> {
                writeUInt(dataSize.toUInt())
            }

            VERSION.DF03 -> {
                writeULong(dataSize)
            }
        }
        write(END_SEQUENCE)
    }

    override fun writeObject(
        sink: Sink,
        obj: Envelope,
    ) {
        val metaFormat = metaFormatFactory.build(io.context, Meta.EMPTY)
        val metaBytes = Binary(obj.meta, metaFormat)
        val actualSize: ULong = (obj.data?.size ?: 0).toULong()
        val tag = Tag(metaFormatFactory.key, metaBytes.size.toUInt() + 2u, actualSize)
        sink.writeBinary(tag.toBinary())
        sink.writeBinary(metaBytes)
        sink.writeString("\r\n")
        obj.data?.let {
            sink.writeBinary(it)
        }
    }

    /**
     * Read an envelope from input into memory
     *
     * @param source an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun readObject(source: Source): Envelope {
        val tag = source.readTag(this.version)

        val metaFormat = io.resolveMetaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaBinary = source.readBinary(tag.metaSize.toInt())

        val meta: Meta = metaFormat.readObjectFrom(metaBinary)

        val data = source.readBinary(tag.dataSize.toInt())

        return SimpleEnvelope(meta, data)
    }

    override fun readObject(binary: Binary): Envelope = binary.read {
        val tag = readTag(version)

        val metaFormat = io.resolveMetaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaBinary = readBinary(tag.metaSize.toInt())

        val meta: Meta = metaFormat.readObjectFrom(metaBinary)


        SimpleEnvelope(meta, binary.view((version.tagSize + tag.metaSize).toInt(), tag.dataSize.toInt()))
    }

    private data class Tag(
        val metaFormatKey: Short,
        val metaSize: UInt,
        val dataSize: ULong,
    )

    public enum class VERSION(public val tagSize: UInt) {
        DF02(20u),
        DF03(24u)
    }

    public companion object : EnvelopeFormatFactory {
        private val START_SEQUENCE = "#~".toAsciiByteString()
        private val END_SEQUENCE = "~#\r\n".toAsciiByteString()

        override val name: Name = EnvelopeFormatFactory.ENVELOPE_FACTORY_NAME + "tagged"

        override fun build(context: Context, meta: Meta): EnvelopeFormat {
            val io = context.io

            val metaFormatName = meta["name"].string?.let { Name.parse(it) } ?: JsonMetaFormat.name
            //Check if appropriate factory exists
            io.metaFormatFactories.find { it.name == metaFormatName } ?: error("Meta format could not be resolved")

            val version: VERSION = meta["version"].enum<VERSION>() ?: VERSION.DF02

            return TaggedEnvelopeFormat(io, version)
        }

        private fun Source.readTag(version: VERSION): Tag {
            val start = readByteString(2)
            if (start != START_SEQUENCE) error("The input is not an envelope")
            val versionString = readByteString(4)
            if (version.name.toAsciiByteString() != versionString) error("Wrong version of DataForge: expected $version but found $versionString")
            val metaFormatKey = readShort()
            val metaLength = readUInt()
            val dataLength: ULong = when (version) {
                VERSION.DF02 -> readUInt().toULong()
                VERSION.DF03 -> readULong()
            }
            val end = readByteString(4)
            if (end != END_SEQUENCE) error("The input is not an envelope")
            return Tag(metaFormatKey, metaLength, dataLength)
        }

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? = try {
            binary.read {
                val header = readByteString(6)
                when (header.substring(2, 6).decodeToString()) {
                    VERSION.DF02.name -> TaggedEnvelopeFormat(io, VERSION.DF02)
                    VERSION.DF03.name -> TaggedEnvelopeFormat(io, VERSION.DF03)
                    else -> null
                }
            }
        } catch (ex: Exception) {
            null
        }

        private val default by lazy { build(Global, Meta.EMPTY) }

        override fun readObject(binary: Binary): Envelope =
            default.run { readObject(binary) }

        override fun writeObject(
            sink: Sink,
            obj: Envelope,
        ): Unit = default.run {
            writeObject(sink, obj)
        }

        override fun readObject(source: Source): Envelope = default.readObject(source)
    }

}