package space.kscience.dataforge.io

import io.ktor.utils.io.bits.Memory
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decodeExactBytes
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.ChunkBuffer
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
public fun IOPlugin.readEnvelope(
    binary: Binary,
    readNonEnvelopes: Boolean = false,
    formatPicker: IOPlugin.(Binary) -> EnvelopeFormat? = IOPlugin::peekBinaryEnvelopeFormat,
): Envelope = formatPicker(binary)?.readBinary(binary) ?: if (readNonEnvelopes) {
    // if no format accepts file, read it as binary
    SimpleEnvelope(Meta.EMPTY, binary)
} else error("Can't infer format for $binary")

@DFExperimental
public fun IOPlugin.readEnvelope(
    string: String,
    readNonEnvelopes: Boolean = false,
    formatPicker: IOPlugin.(Binary) -> EnvelopeFormat? = IOPlugin::peekBinaryEnvelopeFormat,
): Envelope = readEnvelope(string.encodeToByteArray().asBinary(), readNonEnvelopes, formatPicker)


private class RingByteArray(
    private val buffer: ByteArray,
    private var startIndex: Int = 0,
    var size: Int = 0,
) {
    operator fun get(index: Int): Byte {
        require(index >= 0) { "Index must be positive" }
        require(index < size) { "Index $index is out of circular buffer size $size" }
        return buffer[startIndex.forward(index)]
    }

    fun isFull(): Boolean = size == buffer.size

    fun push(element: Byte) {
        buffer[startIndex.forward(size)] = element
        if (isFull()) startIndex++ else size++

    }

    private fun Int.forward(n: Int): Int = (this + n) % (buffer.size)

    fun compare(inputArray: ByteArray): Boolean = when {
        inputArray.size != buffer.size -> false
        size < buffer.size -> false
        else -> inputArray.indices.all { inputArray[it] == get(it) }
    }
}

/**
 * Read [Input] into [output] until designated multy-byte [separator] and optionally continues until
 * the end of the line after it. Throw error if [separator] not found and [atMost] bytes are read.
 * Also fails if [separator] not found until the end of input.
 *
 * Separator itself is not read into Output.
 *
 * @return bytes actually being read, including separator
 */
public fun Input.readBytesWithSeparatorTo(
    output: Output,
    separator: ByteArray,
    atMost: Int = Int.MAX_VALUE,
    skipUntilEndOfLine: Boolean = false,
): Int {
    var counter = 0
    val rb = RingByteArray(ByteArray(separator.size))
    var separatorFound = false
    takeWhile { buffer ->
        while (buffer.canRead()) {
            val byte = buffer.readByte()
            counter++
            if (counter >= atMost) error("Maximum number of bytes to be read $atMost reached.")
            //If end-of-line-search is on, terminate
            if (separatorFound) {
                if (endOfInput || byte == '\n'.code.toByte()) {
                    return counter
                }
            } else {
                rb.push(byte)
                if (rb.compare(separator)) {
                    separatorFound = true
                    if (!skipUntilEndOfLine) {
                        return counter
                    }
                } else if (rb.isFull()) {
                    output.writeByte(rb[0])
                }
            }
        }
        !endOfInput
    }
    error("Read to the end of input without encountering ${separator.decodeToString()}")
}

public fun Input.discardWithSeparator(
    separator: ByteArray,
    atMost: Int = Int.MAX_VALUE,
    skipUntilEndOfLine: Boolean = false,
): Int {
    val dummy: Output = object :Output(ChunkBuffer.Pool){
        override fun closeDestination() {
            // Do nothing
        }

        override fun flush(source: Memory, offset: Int, length: Int) {
            // Do nothing
        }
    }

    return readBytesWithSeparatorTo(dummy, separator, atMost, skipUntilEndOfLine)
}
