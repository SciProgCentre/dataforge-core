package hep.dataforge.io

import kotlinx.io.core.*
import kotlin.math.min

/**
 * A source of binary data
 */
interface Binary {
    /**
     * The size of binary in bytes. [ULong.MAX_VALUE] if size is not defined and input should be read until its end is reached
     */
    val size: ULong

    /**
     * Read continuous [Input] from this binary stating from the beginning.
     * The input is automatically closed on scope close.
     * Some implementation may forbid this to be called twice. In this case second call will throw an exception.
     */
    fun <R> read(block: Input.() -> R): R

    companion object {
        val EMPTY = EmptyBinary
    }
}

/**
 * A [Binary] with addition random access functionality. It by default allows multiple [read] operations.
 */
@ExperimentalUnsignedTypes
interface RandomAccessBinary : Binary {
    /**
     * Read at most [size] of bytes starting at [from] offset from the beginning of the binary.
     * This method could be called multiple times simultaneously.
     *
     * If size
     */
    fun <R> read(from: UInt, size: UInt = UInt.MAX_VALUE, block: Input.() -> R): R

    override fun <R> read(block: Input.() -> R): R = read(0.toUInt(), UInt.MAX_VALUE, block)
}

fun Binary.toBytes(): ByteArray = read {
    this.readBytes()
}

@ExperimentalUnsignedTypes
fun RandomAccessBinary.readPacket(from: UInt, size: UInt): ByteReadPacket = read(from, size) {
    buildPacket { copyTo(this) }
}

@ExperimentalUnsignedTypes
object EmptyBinary : RandomAccessBinary {

    override val size: ULong = 0u

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        error("The binary is empty")
    }
}

@ExperimentalUnsignedTypes
inline class ArrayBinary(val array: ByteArray) : RandomAccessBinary {
    override val size: ULong get() = array.size.toULong()

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        val theSize = min(size, array.size.toUInt() - from)
        return buildPacket {
            writeFully(array, from.toInt(), theSize.toInt())
        }.block()
    }
}

fun ByteArray.asBinary() = ArrayBinary(this)

/**
 * Read given binary as object using given format
 */
fun <T : Any> Binary.readWith(format: IOFormat<T>): T = format.run {
    read {
        readObject()
    }
}

//fun <T : Any> IOFormat<T>.writeBinary(obj: T): Binary {
//    val packet = buildPacket {
//        writeObject(obj)
//    }
//    return ArrayBinary(packet.readBytes())
//}