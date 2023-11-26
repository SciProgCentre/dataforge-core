package space.kscience.dataforge.io

import kotlinx.io.buffered


fun <T : Any> IOFormat<T>.writeToByteArray(obj: T): ByteArray = ByteArray {
    writeTo(this, obj)
}
fun <T : Any> IOFormat<T>.readFromByteArray(array: ByteArray): T = ByteArraySource(array).buffered().use {
    readFrom(it)
}