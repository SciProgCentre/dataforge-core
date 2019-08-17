package hep.dataforge.io

import kotlinx.io.core.*

/**
 * And interface for serialization facilities
 */
interface IOFormat<T : Any> {
    fun Output.writeThis(obj: T)
    fun Input.readThis(): T
}

fun <T : Any> IOFormat<T>.writePacket(obj: T): ByteReadPacket = buildPacket { writeThis(obj) }
fun <T : Any> IOFormat<T>.writeBytes(obj: T): ByteArray = buildPacket { writeThis(obj) }.readBytes()