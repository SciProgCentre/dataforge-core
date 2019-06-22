package hep.dataforge.io

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import kotlinx.io.core.readBytes

/**
 * A source of binary data
 */

interface Binary {
    /**
     * The size of binary in bytes
     */
    val size: ULong

    /**
     * Read continuous [Input] from this binary stating from the beginning.
     * The input is automatically closed on scope close.
     * Some implementation may forbid this to be called twice. In this case second call will throw an exception.
     */
    fun <R> read(block: Input.() -> R): R
}

/**
 * A [Binary] with addition random access functionality. It by default allows multiple [read] operations.
 */
interface RandomAccessBinary : Binary {
    /**
     * Read at most [size] of bytes starting at [from] offset from the beginning of the binary.
     * This method could be called multiple times simultaneously.
     */
    fun <R> read(from: UInt, size: UInt = UInt.MAX_VALUE, block: Input.() -> R): R

    override fun <R> read(block: Input.() -> R): R = read(0.toUInt(), UInt.MAX_VALUE, block)
}

fun Binary.readAll(): ByteReadPacket = read {
    ByteReadPacket(this.readBytes())
}

fun RandomAccessBinary.readPacket(from: UInt, size: UInt): ByteReadPacket = read(from, size) {
    ByteReadPacket(this.readBytes())
}

object EmptyBinary : RandomAccessBinary {

    override val size: ULong = 0.toULong()

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        error("The binary is empty")
    }
}

class ArrayBinary(val array: ByteArray) : RandomAccessBinary {
    override val size: ULong = array.size.toULong()

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        return ByteReadPacket(array, from.toInt(), size.toInt()).block()
    }
}