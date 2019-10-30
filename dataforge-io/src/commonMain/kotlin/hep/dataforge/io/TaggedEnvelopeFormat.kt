package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import kotlinx.io.charsets.Charsets
import kotlinx.io.core.*

@ExperimentalUnsignedTypes
class TaggedEnvelopeFormat(
    val io: IOPlugin,
    private val metaFormatKey: Short
) : EnvelopeFormat {

    private val metaFormat = io.metaFormat(metaFormatKey)
        ?: error("Meta format with key $metaFormatKey could not be resolved in $io")


    private fun Tag.toBytes(): ByteReadPacket = buildPacket(24) {
        writeText(START_SEQUENCE)
        writeText(VERSION)
        writeShort(metaFormatKey)
        writeUInt(metaSize)
        writeULong(dataSize)
        writeText(END_SEQUENCE)
    }

    override fun Output.writeObject(obj: Envelope) {
        val metaBytes = metaFormat.writeBytes(obj.meta)
        val tag = Tag(metaFormatKey, metaBytes.size.toUInt() + 2u, obj.data?.size ?: 0.toULong())
        writePacket(tag.toBytes())
        writeFully(metaBytes)
        writeText("\r\n")
        obj.data?.read { copyTo(this@writeObject) }
        flush()
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun Input.readObject(): Envelope {
        val tag = readTag()

        val metaFormat = io.metaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val dataBytes = readBytes(tag.dataSize.toInt())

        val meta = metaFormat.run { metaPacket.readObject() }
        return SimpleEnvelope(meta, ArrayBinary(dataBytes))
    }

    override fun Input.readPartial(): PartialEnvelope {
        val tag = readTag()

        val metaFormat = io.metaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val meta = metaFormat.run { metaPacket.readObject() }

        return PartialEnvelope(meta, TAG_SIZE + tag.metaSize, tag.dataSize)
    }

    private data class Tag(
        val metaFormatKey: Short,
        val metaSize: UInt,
        val dataSize: ULong
    )

    companion object : EnvelopeFormatFactory {
        const val VERSION = "DF03"
        private const val START_SEQUENCE = "#~"
        private const val END_SEQUENCE = "~#\r\n"
        private const val TAG_SIZE = 24u

        override val name: Name = super.name + VERSION

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            val io = context.io

            val metaFormatName = meta["name"].string?.toName() ?: JsonMetaFormat.name
            val metaFormatFactory = io.metaFormatFactories.find { it.name == metaFormatName }
                ?: error("Meta format could not be resolved")

            return TaggedEnvelopeFormat(io, metaFormatFactory.key)
        }

        private fun Input.readTag(): Tag {
            val start = readTextExactBytes(2, charset = Charsets.ISO_8859_1)
            if (start != START_SEQUENCE) error("The input is not an envelope")
            val version = readTextExactBytes(4, charset = Charsets.ISO_8859_1)
            if (version != VERSION) error("Wrong version of DataForge: expected $VERSION but found $version")
            val metaFormatKey = readShort()
            val metaLength = readUInt()
            val dataLength = readULong()
            val end = readTextExactBytes(4, charset = Charsets.ISO_8859_1)
            if (end != END_SEQUENCE) error("The input is not an envelope")
            return Tag(metaFormatKey, metaLength, dataLength)
        }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                val tag = input.readTag()
                TaggedEnvelopeFormat(io, tag.metaFormatKey)
            } catch (ex: Exception) {
                null
            }
        }

        val default by lazy {  invoke()}
    }

}