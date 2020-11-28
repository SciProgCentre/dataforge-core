package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.IOFormat.Companion.META_KEY
import hep.dataforge.io.IOFormat.Companion.NAME_KEY
import hep.dataforge.meta.Meta
import hep.dataforge.meta.enum
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import kotlinx.io.*

/**
 * A streaming-friendly envelope format with a short binary tag.
 * TODO add description
 */
public class TaggedEnvelopeFormat(
    public val io: IOPlugin,
    public val version: VERSION = VERSION.DF02,
) : EnvelopeFormat {

//    private val metaFormat = io.metaFormat(metaFormatKey)
//        ?: error("Meta format with key $metaFormatKey could not be resolved in $io")


    private fun Tag.toBinary() = Binary(24) {
        writeRawString(START_SEQUENCE)
        writeRawString(version.name)
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
        writeRawString(END_SEQUENCE)
    }

    override fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory,
        formatMeta: Meta,
    ) {
        val metaFormat = metaFormatFactory.invoke(formatMeta, this@TaggedEnvelopeFormat.io.context)
        val metaBytes = metaFormat.toBinary(envelope.meta)
        val actualSize: ULong = (envelope.data?.size ?: 0).toULong()
        val tag = Tag(metaFormatFactory.key, metaBytes.size.toUInt() + 2u, actualSize)
        output.writeBinary(tag.toBinary())
        output.writeBinary(metaBytes)
        output.writeRawString("\r\n")
        envelope.data?.let {
            output.writeBinary(it)
        }
        output.flush()
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun readObject(input: Input): Envelope {
        val tag = input.readTag(this.version)

        val metaFormat = io.resolveMetaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val meta: Meta = metaFormat.readObject(input.limit(tag.metaSize.toInt()))

        val data = input.readBinary(tag.dataSize.toInt())

        return SimpleEnvelope(meta, data)
    }

    override fun readPartial(input: Input): PartialEnvelope {
        val tag = input.readTag(this.version)

        val metaFormat = io.resolveMetaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val meta: Meta = metaFormat.readObject(input.limit(tag.metaSize.toInt()))


        return PartialEnvelope(meta, version.tagSize + tag.metaSize, tag.dataSize)
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

    override fun toMeta(): Meta = Meta {
        NAME_KEY put name.toString()
        META_KEY put {
            "version" put version
        }
    }

    public companion object : EnvelopeFormatFactory {
        private const val START_SEQUENCE = "#~"
        private const val END_SEQUENCE = "~#\r\n"

        override val name: Name = super.name + "tagged"

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            val io = context.io

            val metaFormatName = meta["name"].string?.toName() ?: JsonMetaFormat.name
            //Check if appropriate factory exists
            io.metaFormatFactories.find { it.name == metaFormatName } ?: error("Meta format could not be resolved")

            val version: VERSION = meta["version"].enum<VERSION>() ?: VERSION.DF02

            return TaggedEnvelopeFormat(io, version)
        }

        private fun Input.readTag(version: VERSION): Tag {
            val start = readRawString(2)
            if (start != START_SEQUENCE) error("The input is not an envelope")
            val versionString = readRawString(4)
            if (version.name != versionString) error("Wrong version of DataForge: expected $version but found $versionString")
            val metaFormatKey = readShort()
            val metaLength = readUInt()
            val dataLength: ULong = when (version) {
                VERSION.DF02 -> readUInt().toULong()
                VERSION.DF03 -> readULong()
            }
            val end = readRawString(4)
            if (end != END_SEQUENCE) error("The input is not an envelope")
            return Tag(metaFormatKey, metaLength, dataLength)
        }

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                input.preview {
                    val header = readRawString(6)
                    return@preview when (header.substring(2..5)) {
                        VERSION.DF02.name -> TaggedEnvelopeFormat(io, VERSION.DF02)
                        VERSION.DF03.name -> TaggedEnvelopeFormat(io, VERSION.DF03)
                        else -> null
                    }
                }
            } catch (ex: Exception) {
                null
            }
        }

        private val default by lazy { invoke() }

        override fun readPartial(input: Input): PartialEnvelope =
            default.run { readPartial(input) }

        override fun writeEnvelope(
            output: Output,
            envelope: Envelope,
            metaFormatFactory: MetaFormatFactory,
            formatMeta: Meta,
        ): Unit = default.run {
            writeEnvelope(
                output,
                envelope,
                metaFormatFactory,
                formatMeta
            )
        }

        override fun readObject(input: Input): Envelope = default.readObject(input)
    }

}