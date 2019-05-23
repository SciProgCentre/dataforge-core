package hep.dataforge.io

import kotlinx.io.core.*

/**
 * And interface for serialization facilities
 */
interface IOFormat<T : Any> {
    fun Output.writeObject(obj: T)
    fun Input.readObject(): T
}

fun <T : Any> IOFormat<T>.writePacket(obj: T): ByteReadPacket = buildPacket { writeObject(obj) }
fun <T : Any> IOFormat<T>.writeBytes(obj: T): ByteArray = buildPacket { writeObject(obj) }.readBytes()