package hep.dataforge.io

import hep.dataforge.names.Name
import hep.dataforge.names.plus
import kotlinx.io.core.*


@ExperimentalUnsignedTypes
object TaggedEnvelopeFormat : EnvelopeFormat {
    const val VERSION = "DF03"
    private const val START_SEQUENCE = "#~"
    private const val END_SEQUENCE = "~#\r\n"
    private const val TAG_SIZE = 26u

    override val name: Name = super.name + VERSION

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
        return Tag(metaFormatKey, metaLength, dataLength)
    }

    override fun Output.writeEnvelope(envelope: Envelope, format: MetaFormat) {
        val metaBytes = format.writeBytes(envelope.meta)
        val tag = Tag(format.key, metaBytes.size.toUInt(), envelope.data?.size ?: 0.toULong())
        writePacket(tag.toBytes())
        writeFully(metaBytes)
        envelope.data?.read { copyTo(this@writeEnvelope) }
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun Input.readEnvelope(formats: Collection<MetaFormat>): Envelope {
        val tag = readTag()

        val metaFormat = formats.find { it.key == tag.metaFormatKey }
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val meta = metaFormat.run { metaPacket.readThis() }

        val dataBytes = readBytes(tag.dataSize.toInt())

        return SimpleEnvelope(meta, ArrayBinary(dataBytes))
    }

    override fun Input.readPartial(formats: Collection<MetaFormat>): PartialEnvelope {
        val tag = readTag()

        val metaFormat = formats.find { it.key == tag.metaFormatKey }
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

}