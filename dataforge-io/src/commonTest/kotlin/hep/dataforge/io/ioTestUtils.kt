package hep.dataforge.io

import kotlinx.io.ByteArrayInput
import kotlinx.io.use

fun <T : Any> IOFormat<T>.toByteArray(obj: T): ByteArray = buildByteArray { writeObject(this, obj) }
fun <T : Any> IOFormat<T>.readByteArray(array: ByteArray): T = ByteArrayInput(array).use { readObject(it) }