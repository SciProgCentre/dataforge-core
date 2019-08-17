package hep.dataforge.io

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import kotlinx.io.core.buildPacket
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
@ExperimentalUnsignedTypes
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

@ExperimentalUnsignedTypes
fun RandomAccessBinary.readPacket(from: UInt, size: UInt): ByteReadPacket = read(from, size) {
    ByteReadPacket(this.readBytes())
}

@ExperimentalUnsignedTypes
object EmptyBinary : RandomAccessBinary {

    override val size: ULong = 0.toULong()

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        error("The binary is empty")
    }
}

@ExperimentalUnsignedTypes
class ArrayBinary(val array: ByteArray) : RandomAccessBinary {
    override val size: ULong get() = array.size.toULong()

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        return ByteReadPacket(array, from.toInt(), size.toInt()).block()
    }
}

/**
 * Read given binary as object using given format
 */
fun <T : Any> Binary.readWith(format: IOFormat<T>): T = format.run {
    read {
        readThis()
    }
}

/**
 * Write this object to a binary
 * TODO make a lazy binary that does not use intermediate array
 */
fun <T: Any> T.writeWith(format: IOFormat<T>): Binary = format.run{
    val packet = buildPacket {
        writeThis(this@writeWith)
    }
    return@run ArrayBinary(packet.readBytes())
}