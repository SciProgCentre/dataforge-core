package space.kscience.dataforge.io

import io.ktor.utils.io.core.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.io.IOFormat.Companion.META_KEY
import space.kscience.dataforge.io.IOFormat.Companion.NAME_KEY
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
        val metaFormat = metaFormatFactory.build(this@TaggedEnvelopeFormat.io.context, formatMeta)
        val metaBytes = metaFormat.toBinary(envelope.meta)
        val actualSize: ULong = (envelope.data?.size ?: 0).toULong()
        val tag = Tag(metaFormatFactory.key, metaBytes.size.toUInt() + 2u, actualSize)
        output.writeBinary(tag.toBinary())
        output.writeBinary(metaBytes)
        output.writeRawString("\r\n")
        envelope.data?.let {
            output.writeBinary(it)
        }
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

        val metaBinary = input.readBinary(tag.metaSize.toInt())

        val meta: Meta = metaFormat.readObject(metaBinary)

        val data = input.readBinary(tag.dataSize.toInt())

        return SimpleEnvelope(meta, data)
    }

    override fun readPartial(input: Input): PartialEnvelope {
        val tag = input.readTag(this.version)

        val metaFormat = io.resolveMetaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val metaBinary = input.readBinary(tag.metaSize.toInt())

        val meta: Meta = metaFormat.readObject(metaBinary)


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

        override fun build(context: Context, meta: Meta): EnvelopeFormat {
            val io = context.io

            val metaFormatName = meta["name"].string?.let { Name.parse(it) } ?: JsonMetaFormat.name
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

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? {
            return try {
                binary.read{
                    val header = readRawString(6)
                    return@read when (header.substring(2..5)) {
                        VERSION.DF02.name -> TaggedEnvelopeFormat(io, VERSION.DF02)
                        VERSION.DF03.name -> TaggedEnvelopeFormat(io, VERSION.DF03)
                        else -> null
                    }
                }
            } catch (ex: Exception) {
                null
            }
        }

        private val default by lazy { build(Global, Meta.EMPTY) }

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