package hep.dataforge.io

import kotlinx.io.core.*

class TaggedEnvelopeFormat(
    val metaFormats: Collection<MetaFormat>,
    val outputMetaFormat: MetaFormat = metaFormats.first()
) : EnvelopeFormat {

    override fun Output.writeObject(obj: Envelope) {
        write(obj, this, outputMetaFormat)
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param metaFormats a collection of meta formats to resolve
     */
    override fun Input.readObject(): Envelope = read(this, metaFormats)


    private data class Tag(
        val metaFormatKey: Short,
        val metaSize: UInt,
        val dataSize: ULong
    )

    companion object {
        private const val VERSION = "DF03"
        private const val START_SEQUENCE = "#~"
        private const val END_SEQUENCE = "~#\r\n"

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

        fun write(obj: Envelope, out: Output, metaFormat: MetaFormat) {
            val metaBytes = metaFormat.writeBytes(obj.meta)
            val tag = Tag(metaFormat.key, metaBytes.size.toUInt(), obj.data?.size ?: 0.toULong())
            out.writePacket(tag.toBytes())
            out.writeFully(metaBytes)
            obj.data?.read {
                while (!endOfInput){
                    out.writeByte(readByte())
                }
            }
        }
    }
}