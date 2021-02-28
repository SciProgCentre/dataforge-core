package hep.dataforge.io

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decodeExactBytes
import io.ktor.utils.io.core.*
import kotlin.math.min

public fun Output.writeRawString(str: String) {
    writeFully(str.toByteArray(Charsets.ISO_8859_1))
}

public fun Output.writeUtf8String(str: String) {
    writeFully(str.encodeToByteArray())
}

@OptIn(ExperimentalIoApi::class)
public fun Input.readRawString(size: Int): String {
    return Charsets.ISO_8859_1.newDecoder().decodeExactBytes(this, size)
}

public fun Input.readUtf8String(): String = readBytes().decodeToString()

public fun Input.readSafeUtf8Line(): String = readUTF8Line() ?: error("Line not found")

public inline fun buildByteArray(expectedSize: Int = 16, block: Output.() -> Unit): ByteArray {
    val builder = BytePacketBuilder(expectedSize)
    builder.block()
    return builder.build().readBytes()
}

public inline fun Binary(expectedSize: Int = 16, block: Output.() -> Unit): Binary =
    buildByteArray(expectedSize, block).asBinary()

/**
 * View section of a [Binary] as an independent binary
 */
public class BinaryView(private val source: Binary, private val start: Int, override val size: Int) : Binary {

    init {
        require(start > 0)
        require(start + size <= source.size) { "View boundary is outside source binary size" }
    }

    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R {
        return source.read(start + offset, min(size, atMost), block)
    }
}

public fun Binary.view(start: Int, size: Int): BinaryView = BinaryView(this, start, size)

public operator fun Binary.get(range: IntRange): BinaryView = view(range.first, range.last - range.first)