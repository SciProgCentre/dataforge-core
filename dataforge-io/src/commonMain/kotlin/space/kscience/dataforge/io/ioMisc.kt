package space.kscience.dataforge.io

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import kotlin.math.min

/**
 * Convert a string literal, containing only ASCII characters to a [ByteString].
 * Throws an error if there are non-ASCII characters.
 */
public fun String.toAsciiByteString(): ByteString {
    val bytes = ByteArray(length) {
        val char = get(it)
        val code = char.code
        if (code > Byte.MAX_VALUE) error("Symbol $char is not ASCII symbol") else code.toByte()
    }
    return ByteString(bytes)
}

public inline fun Buffer(block: Sink.() -> Unit): Buffer = Buffer().apply(block)

//public fun Source.readSafeUtf8Line(): String = readUTF8Line() ?: error("Line not found")

public inline fun ByteArray(block: Sink.() -> Unit): ByteArray =
    Buffer(block).readByteArray()

public inline fun Binary(block: Sink.() -> Unit): Binary =
    ByteArray(block).asBinary()

public operator fun Binary.get(range: IntRange): Binary = view(range.first, range.last - range.first)

/**
 * Return inferred [EnvelopeFormat] if only one format could read given file. If no format accepts the binary, return null. If
 * multiple formats accept binary, throw an error.
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
 * A zero-copy read from
 */
@DFExperimental
public fun IOPlugin.readEnvelope(
    binary: Binary,
    readNonEnvelopes: Boolean = false,
    formatPicker: IOPlugin.(Binary) -> EnvelopeFormat? = IOPlugin::peekBinaryEnvelopeFormat,
): Envelope = formatPicker(binary)?.readFrom(binary) ?: if (readNonEnvelopes) {
    // if no format accepts file, read it as binary
    Envelope(Meta.EMPTY, binary)
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

    fun contentEquals(inputArray: ByteArray): Boolean = when {
        inputArray.size != buffer.size -> false
        size < buffer.size -> false
        else -> inputArray.indices.all { inputArray[it] == get(it) }
    }

    fun contentEquals(byteString: ByteString): Boolean = when {
        byteString.size != buffer.size -> false
        size < buffer.size -> false
        else -> (0 until byteString.size).all { byteString[it] == get(it) }
    }

}

private fun RingByteArray.toArray(): ByteArray = ByteArray(size) { get(it) }

/**
 * Read [Source] into [output] until designated multibyte [separator] and optionally continues until
 * the end of the line after it. Throw error if [separator] not found and [atMost] bytes are read.
 * Also fails if [separator] not found until the end of input.
 *
 * The Separator itself is not read into [Sink].
 *
 * @param errorOnEof if true error is thrown if separator is never encountered
 *
 * @return bytes actually being read, including separator
 */
public fun Source.readWithSeparatorTo(
    output: Sink?,
    separator: ByteString,
    atMost: Int = Int.MAX_VALUE,
    errorOnEof: Boolean = false,
): Int {
    var counter = 0
    val rb = RingByteArray(ByteArray(separator.size))

    while (!exhausted()) {
        val byte = readByte()
        counter++
        if (counter >= atMost) error("Maximum number of bytes to be read $atMost reached.")
        rb.push(byte)
        if (rb.contentEquals(separator)) {
            return counter
        } else if (rb.isFull()) {
            output?.writeByte(rb[0])
        }
    }

    if (errorOnEof) {
        error("Read to the end of input without encountering ${separator.decodeToString()}")
    } else {
        for (i in 1 until rb.size) {
            output?.writeByte(rb[i])
        }
        counter += (rb.size - 1)
        return counter
    }
}

/**
 * Discard all bytes until [separator] is encountered. Separator is discarded sa well.
 * Return the total number of bytes read.
 */
public fun Source.discardWithSeparator(
    separator: ByteString,
    atMost: Int = Int.MAX_VALUE,
    errorOnEof: Boolean = false,
): Int = readWithSeparatorTo(null, separator, atMost, errorOnEof)

/**
 * Discard all symbol until newline is discovered. Carriage return is not discarded.
 */
public fun Source.discardLine(
    atMost: Int = Int.MAX_VALUE,
    errorOnEof: Boolean = false,
): Int = discardWithSeparator("\n".encodeToByteString(), atMost, errorOnEof)


/**
 * A [Source] based on [ByteArray]
 */
internal class ByteArraySource(
    private val byteArray: ByteArray,
    private val offset: Int = 0,
    private val size: Int = byteArray.size - offset,
) : RawSource {

    init {
        require(offset >= 0) { "Offset must be positive" }
        require(offset + size <= byteArray.size) { "End index is ${offset + size}, but the array size is ${byteArray.size}" }
    }

    private var pointer = offset

    override fun close() {
        // Do nothing
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (pointer == offset + size) return -1
        val byteRead = min(byteCount.toInt(), (size + offset - pointer))
        sink.write(byteArray, pointer, pointer + byteRead)
        pointer += byteRead
        return byteRead.toLong()
    }
}

/**
 * A [Source] based on [String]
 */
public class StringSource(
    public val string: String,
    public val offset: Int = 0,
    public val size: Int = string.length - offset,
) : RawSource {

    private var pointer = offset

    override fun close() {
        // Do nothing
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (pointer == offset + size) return -1
        val byteRead = min(byteCount.toInt(), (size + offset - pointer))
        sink.writeString(string, pointer, pointer + byteRead)
        pointer += byteRead
        return byteRead.toLong()
    }
}

public fun Sink.writeDouble(value: Double) {
    writeLong(value.toBits())
}

public fun Source.readDouble(): Double = Double.fromBits(readLong())

public fun Sink.writeFloat(value: Float) {
    writeInt(value.toBits())
}

public fun Source.readFloat(): Float = Float.fromBits(readInt())