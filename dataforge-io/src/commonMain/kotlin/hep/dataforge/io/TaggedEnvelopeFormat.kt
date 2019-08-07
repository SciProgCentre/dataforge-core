package hep.dataforge.io

import kotlinx.io.core.*

class TaggedEnvelopeFormat(val metaFormats: Collection<MetaFormat>) : EnvelopeFormat {

    override fun Output.writeEnvelope(envelope: Envelope, format: MetaFormat) {
        write(this, envelope, format)
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param metaFormats a collection of meta formats to resolve
     */
    override fun Input.readObject(): Envelope = read(this, metaFormats)

    override fun readPartial(input: Input): PartialEnvelope = Companion.readPartial(input, metaFormats)

    private data class Tag(
        val metaFormatKey: Short,
        val metaSize: UInt,
        val dataSize: ULong
    )

    companion object {
        const val VERSION = "DF03"
        private const val START_SEQUENCE = "#~"
        private const val END_SEQUENCE = "~#\r\n"
        private const val TAG_SIZE = 26u

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

        fun read(input: Input, metaFormats: Collection<MetaFormat>): Envelope {
            val tag = input.readTag()

            val metaFormat = metaFormats.find { it.key == tag.metaFormatKey }
                ?: error("Meta format with key ${tag.metaFormatKey} not found")

            val metaPacket = ByteReadPacket(input.readBytes(tag.metaSize.toInt()))
            val meta = metaFormat.run { metaPacket.readObject() }

            val dataBytes = input.readBytes(tag.dataSize.toInt())

            return SimpleEnvelope(meta, ArrayBinary(dataBytes))
        }

        fun readPartial(input: Input, metaFormats: Collection<MetaFormat>): PartialEnvelope {
            val tag = input.readTag()

            val metaFormat = metaFormats.find { it.key == tag.metaFormatKey }
                ?: error("Meta format with key ${tag.metaFormatKey} not found")

            val metaPacket = ByteReadPacket(input.readBytes(tag.metaSize.toInt()))
            val meta = metaFormat.run { metaPacket.readObject() }

            return PartialEnvelope(meta, TAG_SIZE + tag.metaSize, tag.dataSize)
        }

        fun write(out: Output, envelope: Envelope, metaFormat: MetaFormat) {
            val metaBytes = metaFormat.writeBytes(envelope.meta)
            val tag = Tag(metaFormat.key, metaBytes.size.toUInt(), envelope.data?.size ?: 0.toULong())
            out.writePacket(tag.toBytes())
            out.writeFully(metaBytes)
            envelope.data?.read { copyTo(out) }
        }
    }
}