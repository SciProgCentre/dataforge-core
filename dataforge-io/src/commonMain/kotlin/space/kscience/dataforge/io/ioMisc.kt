package space.kscience.dataforge.io

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental

/**
 * Convert a string literal, containing only ASCII characters to a [ByteString].
 * Throws an error if there are non-ASCII characters.
 */
public fun String.toACIIByteString(): ByteString {
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
): Envelope = formatPicker(binary)?.readObject(binary) ?: if (readNonEnvelopes) {
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

}

private fun RingByteArray.toArray(): ByteArray = ByteArray(size) { get(it) }

/**
 * Read [Input] into [output] until designated multibyte [separator] and optionally continues until
 * the end of the line after it. Throw error if [separator] not found and [atMost] bytes are read.
 * Also fails if [separator] not found until the end of input.
 *
 * Separator itself is not read into Output.
 *
 * @param errorOnEof if true error is thrown if separator is never encountered
 *
 * @return bytes actually being read, including separator
 */
public fun Source.readWithSeparatorTo(
    output: Sink,
    separator: ByteArray,
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
            output.writeByte(rb[0])
        }
    }

    if (errorOnEof) {
        error("Read to the end of input without encountering ${separator.decodeToString()}")
    } else {
        for (i in 1 until rb.size) {
            output.writeByte(rb[i])
        }
        counter += (rb.size - 1)
        return counter
    }
}

public fun Source.discardLine(): Int {
    return discardUntilDelimiter('\n'.code.toByte()).also {
        discard(1)
    }.toInt() + 1
}

public fun Source.discardWithSeparator(
    separator: ByteArray,
    atMost: Int = Int.MAX_VALUE,
    errorOnEof: Boolean = false,
): Int {
    val dummy: Sink = object : Sink(ChunkBuffer.Pool) {
        override fun closeDestination() {
            // Do nothing
        }

        override fun flush(source: Memory, offset: Int, length: Int) {
            // Do nothing
        }
    }

    return readWithSeparatorTo(dummy, separator, atMost, errorOnEof)
}
