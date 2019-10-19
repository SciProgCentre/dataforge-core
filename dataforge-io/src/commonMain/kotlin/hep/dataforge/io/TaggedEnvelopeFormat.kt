package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import kotlinx.io.core.*

@ExperimentalUnsignedTypes
class TaggedEnvelopeFormat(val io: IOPlugin, meta: Meta) : EnvelopeFormat {

    private val metaFormat: MetaFormat

    private val metaFormatKey: Short

    init {
        val metaName = meta["name"].string?.toName() ?: JsonMetaFormat.name
        val metaFormatFactory = io.metaFormatFactories.find { it.name == metaName }
            ?: error("Meta format could not be resolved")

        metaFormat = metaFormatFactory(meta, io.context)
        metaFormatKey = metaFormatFactory.key
    }

    private fun Tag.toBytes(): ByteReadPacket = buildPacket(24) {
        writeText(START_SEQUENCE)
        writeText(VERSION)
        writeShort(metaFormatKey)
        writeUInt(metaSize)
        writeULong(dataSize)
        writeText(END_SEQUENCE)
    }

    private fun Input.readTag(): Tag {
        val start = readTextExactBytes(2)
        if (start != START_SEQUENCE) error("The input is not an envelope")
        val version = readTextExactBytes(4)
        if (version != VERSION) error("Wrong version of DataForge: expected $VERSION but found $version")
        val metaFormatKey = readShort()
        val metaLength = readUInt()
        val dataLength = readULong()
        val end = readTextExactBytes(4)
        if (end != END_SEQUENCE) error("The input is not an envelope")
        return Tag(metaFormatKey, metaLength, dataLength)
    }

    override fun Output.writeThis(obj: Envelope) {
        val metaBytes = metaFormat.writeBytes(obj.meta)
        val tag = Tag(metaFormatKey, metaBytes.size.toUInt(), obj.data?.size ?: 0.toULong())
        writePacket(tag.toBytes())
        writeFully(metaBytes)
        obj.data?.read { copyTo(this@writeThis) }
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun Input.readThis(): Envelope {
        val tag = readTag()

        val metaFormat = io.metaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val dataBytes = readBytes(tag.dataSize.toInt())

        val meta = metaFormat.run { metaPacket.readThis() }
        return SimpleEnvelope(meta, ArrayBinary(dataBytes))
    }

    override fun Input.readPartial(): PartialEnvelope {
        val tag = readTag()

        val metaFormat = io.metaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val meta = metaFormat.run { metaPacket.readThis() }

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
            val plugin = context.plugins.fetch(IOPlugin)
            return TaggedEnvelopeFormat(plugin, meta)
        }

        val default = invoke()
    }

}