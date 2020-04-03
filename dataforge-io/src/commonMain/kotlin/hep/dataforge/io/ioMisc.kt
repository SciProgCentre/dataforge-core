package hep.dataforge.io

import kotlinx.io.*

fun Output.writeRawString(str: String) {
    str.forEach { writeByte(it.toByte()) }
}

fun Input.readRawString(size: Int): String {
    val array = CharArray(size) { readByte().toChar() }
    return String(array)
}

inline fun buildByteArray(expectedSize: Int = 16, block: Output.() -> Unit): ByteArray =
    ByteArrayOutput(expectedSize).apply(block).toByteArray()

@Suppress("FunctionName")
inline fun Binary(expectedSize: Int = 16, block: Output.() -> Unit): Binary =
    buildByteArray(expectedSize, block).asBinary()

@Deprecated("To be replaced by Binary.EMPTY",level = DeprecationLevel.WARNING)
val EmptyBinary = ByteArrayBinary(ByteArray(0))