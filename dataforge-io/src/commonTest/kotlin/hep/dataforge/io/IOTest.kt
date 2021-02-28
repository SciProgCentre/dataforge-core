package hep.dataforge.io

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class IOTest {
    @Test
    fun readBytes() {
        val bytes = ByteArray(8) { it.toByte() }
        val input = ByteReadPacket(bytes)
        val first = input.readBytes(4)
        val second = input.readBytes(4)
        assertEquals(4.toByte(), second[0])
    }
}