package space.kscience.dataforge.io

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decodeExactBytes
import io.ktor.utils.io.core.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import kotlin.math.min

public fun Output.writeRawString(str: String) {
    writeFully(str.toByteArray(Charsets.ISO_8859_1))
}

public fun Output.writeUtf8String(str: String) {
    writeFully(str.encodeToByteArray())
}

public fun Input.readRawString(size: Int): String {
    return Charsets.ISO_8859_1.newDecoder().decodeExactBytes(this, size)
}

public fun Input.readUtf8String(): String = readBytes().decodeToString()

public fun Input.readSafeUtf8Line(): String = readUTF8Line() ?: error("Line not found")

public inline fun ByteArray(block: Output.() -> Unit): ByteArray =
    buildPacket(block).readBytes()

public inline fun Binary(block: Output.() -> Unit): Binary =
    ByteArray(block).asBinary()

/**
 * View section of a [Binary] as an independent binary
 */
public class BinaryView(private val source: Binary, private val start: Int, override val size: Int) : Binary {

    init {
        require(start > 0)
        require(start + size <= source.size) { "View boundary is outside source binary size" }
    }

    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R =
        source.read(start + offset, min(size, atMost), block)

    override suspend fun <R> readSuspend(offset: Int, atMost: Int, block: suspend Input.() -> R): R =
        source.readSuspend(start + offset, min(size, atMost), block)
}

public fun Binary.view(start: Int, size: Int): BinaryView = BinaryView(this, start, size)

public operator fun Binary.get(range: IntRange): BinaryView = view(range.first, range.last - range.first)

/**
 * Return inferred [EnvelopeFormat] if only one format could read given file. If no format accepts the binary, return null. If
 * multiple formats accepts binary, throw an error.
 */
public fun IOPlugin.peekBinaryEnvelopeFormat(binary: Binary): EnvelopeFormat? {
    val formats = envelopeFormatFactories.mapNotNull { factory ->
        factory.peekFormat(this@peekBinaryEnvelopeFormat, binary)
    }

    return when (formats.size) {
        0 -> null
        1 -> formats.first()
        else -> error("Envelope format binary recognition clash: $formats")
    }
}

/**
 * Zero-copy read this binary as an envelope using given [this@toEnvelope]
 */
@DFExperimental
public fun EnvelopeFormat.readBinary(binary: Binary): Envelope {
    val partialEnvelope: PartialEnvelope = binary.read {
        run {
            readPartial(this@read)
        }
    }
    val offset: Int = partialEnvelope.dataOffset.toInt()
    val size: Int = partialEnvelope.dataSize?.toInt() ?: (binary.size - offset)
    val envelopeBinary = BinaryView(binary, offset, size)
    return SimpleEnvelope(partialEnvelope.meta, envelopeBinary)
}

/**
 * A zero-copy read from
 */
@DFExperimental
public fun IOPlugin.readEnvelopeBinary(
    binary: Binary,
    readNonEnvelopes: Boolean = false,
    formatPicker: IOPlugin.(Binary) -> EnvelopeFormat? = IOPlugin::peekBinaryEnvelopeFormat,
): Envelope = formatPicker(binary)?.readBinary(binary) ?: if (readNonEnvelopes) {
    // if no format accepts file, read it as binary
    SimpleEnvelope(Meta.EMPTY, binary)
} else error("Can't infer format for $binary")