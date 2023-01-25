package space.kscience.dataforge.io

import io.ktor.utils.io.core.*
import kotlin.math.min

/**
 * [Binary] represents a fixed-size multi-read byte block, which is not attached to the Input which was used to create it.
 * The binary could be associated with a resource, but it should guarantee that when someone is trying to read the binary,
 * this resource is re-acquired.
 */
public interface Binary {

    public val size: Int


    /**
     * Read maximum of [atMost] bytes as input from the binary, starting at [offset]. The generated input is always closed
     * when leaving scope, so it could not be leaked outside of scope of [block].
     */
    public fun <R> read(offset: Int = 0, atMost: Int = size - offset, block: Input.() -> R): R

    public suspend fun <R> readSuspend(offset: Int = 0, atMost: Int = size - offset, block: suspend Input.() -> R): R

    /**
     * Read a binary with given [offset] relative to this binary and given [binarySize].
     * In general, resulting binary is of the same type as this one, but it is not guaranteed.
     */
    public fun view(offset: Int, binarySize: Int = size - offset): Binary

    public companion object {
        public val EMPTY: Binary = ByteArrayBinary(ByteArray(0))
    }
}

internal class ByteArrayBinary(
    internal val array: ByteArray,
    internal val start: Int = 0,
    override val size: Int = array.size - start,
) : Binary {

    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R {
        require(offset >= 0) { "Offset must be positive" }
        require(offset < array.size) { "Offset $offset is larger than array size" }
        val input = ByteReadPacket(
            array,
            offset + start,
            min(atMost, size - offset)
        )
        return input.use(block)
    }

    override suspend fun <R> readSuspend(offset: Int, atMost: Int, block: suspend Input.() -> R): R {
        require(offset >= 0) { "Offset must be positive" }
        require(offset < array.size) { "Offset $offset is larger than array size" }
        val input = ByteReadPacket(
            array,
            offset + start,
            min(atMost, size - offset)
        )
        return try {
            block(input)
        } finally {
            input.close()
        }
    }

    override fun view(offset: Int, binarySize: Int): ByteArrayBinary =
        ByteArrayBinary(array, start + offset, binarySize)
}

public fun ByteArray.asBinary(): Binary = ByteArrayBinary(this)

/**
 * Produce a [ByteArray] representing an exact copy of this [Binary]
 */
public fun Binary.toByteArray(): ByteArray = if (this is ByteArrayBinary) {
    array.copyOfRange(start, start + size) // TODO do we need to ensure data safety here?
} else {
    read {
        readBytes()
    }
}

//TODO optimize for file-based Inputs
public fun Input.readBinary(size: Int? = null): Binary {
    val array = if (size == null) readBytes() else readBytes(size)
    return ByteArrayBinary(array)
}

/**
 * Direct write of binary to the output. Returns the number of bytes written
 */
public fun Output.writeBinary(binary: Binary): Int {
    return if (binary is ByteArrayBinary) {
        writeFully(binary.array, binary.start, binary.start + binary.size)
        binary.size
    } else {
        binary.read {
            copyTo(this@writeBinary).toInt()
        }
    }
}