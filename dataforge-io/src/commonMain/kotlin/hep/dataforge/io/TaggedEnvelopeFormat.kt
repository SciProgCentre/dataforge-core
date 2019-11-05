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
    val version: VERSION = TaggedEnvelopeFormat.VERSION.DF02
) : EnvelopeFormat {

//    private val metaFormat = io.metaFormat(metaFormatKey)
//        ?: error("Meta format with key $metaFormatKey could not be resolved in $io")


    private fun Tag.toBytes(): ByteReadPacket = buildPacket(24) {
        writeText(START_SEQUENCE)
        writeText(version.name)
        writeShort(metaFormatKey)
        writeUInt(metaSize)
        when (version) {
            TaggedEnvelopeFormat.VERSION.DF02 -> {
                writeUInt(dataSize.toUInt())
            }
            TaggedEnvelopeFormat.VERSION.DF03 -> {
                writeULong(dataSize)
            }
        }
        writeText(END_SEQUENCE)
    }

    override fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta) {
        val metaFormat = metaFormatFactory.invoke(formatMeta, io.context)
        val metaBytes = metaFormat.writeBytes(envelope.meta)
        val tag = Tag(metaFormatFactory.key, metaBytes.size.toUInt() + 2u, envelope.data?.size ?: 0.toULong())
        writePacket(tag.toBytes())
        writeFully(metaBytes)
        writeText("\r\n")
        envelope.data?.read { copyTo(this@writeEnvelope) }
        flush()
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun Input.readObject(): Envelope {
        val tag = readTag(version)

        val metaFormat = io.metaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val dataBytes = readBytes(tag.dataSize.toInt())

        val meta = metaFormat.run { metaPacket.readObject() }
        return SimpleEnvelope(meta, ArrayBinary(dataBytes))
    }

    override fun Input.readPartial(): PartialEnvelope {
        val tag = readTag(version)

        val metaFormat = io.metaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaPacket = ByteReadPacket(readBytes(tag.metaSize.toInt()))
        val meta = metaFormat.run { metaPacket.readObject() }

        return PartialEnvelope(meta, version.tagSize + tag.metaSize, tag.dataSize)
    }

    private data class Tag(
        val metaFormatKey: Short,
        val metaSize: UInt,
        val dataSize: ULong
    )

    enum class VERSION(val tagSize: UInt) {
        DF02(20u),
        DF03(24u)
    }

    companion object : EnvelopeFormatFactory {
        private const val START_SEQUENCE = "#~"
        private const val END_SEQUENCE = "~#\r\n"

        override val name: Name = super.name + "tagged"

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            val io = context.io

            val metaFormatName = meta["name"].string?.toName() ?: JsonMetaFormat.name
            val metaFormatFactory = io.metaFormatFactories.find { it.name == metaFormatName }
                ?: error("Meta format could not be resolved")

            return TaggedEnvelopeFormat(io)
        }

        private fun Input.readTag(version: VERSION): Tag {
            val start = readTextExactBytes(2, charset = Charsets.ISO_8859_1)
            if (start != START_SEQUENCE) error("The input is not an envelope")
            val versionString = readTextExactBytes(4, charset = Charsets.ISO_8859_1)
            if (version.name != versionString) error("Wrong version of DataForge: expected $version but found $versionString")
            val metaFormatKey = readShort()
            val metaLength = readUInt()
            val dataLength: ULong = when (version) {
                VERSION.DF02 -> readUInt().toULong()
                VERSION.DF03 -> readULong()
            }
            val end = readTextExactBytes(4, charset = Charsets.ISO_8859_1)
            if (end != END_SEQUENCE) error("The input is not an envelope")
            return Tag(metaFormatKey, metaLength, dataLength)
        }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                val header = input.readTextExactBytes(6)
                when (header.substring(2..5)) {
                    VERSION.DF02.name -> TaggedEnvelopeFormat(io, VERSION.DF02)
                    VERSION.DF03.name -> TaggedEnvelopeFormat(io, VERSION.DF03)
                    else -> null
                }
            } catch (ex: Exception) {
                null
            }
        }

        val default by lazy { invoke() }
    }

}