package hep.dataforge.io

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.use


fun <T : Any> IOFormat<T>.writeToByteArray(obj: T): ByteArray = buildByteArray {
    writeObject(this, obj)
}
fun <T : Any> IOFormat<T>.readFromByteArray(array: ByteArray): T = ByteReadPacket(array).use {
    readObject(it)
}