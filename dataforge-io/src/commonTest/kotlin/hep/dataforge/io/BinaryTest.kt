package hep.dataforge.io

import kotlinx.io.asBinary
import kotlinx.io.readByte
import kotlinx.io.readInt
import kotlin.test.Test
import kotlin.test.assertEquals

class BinaryTest {
    @Test
    fun testBinaryAccess(){
        val binary = ByteArray(128){it.toByte()}.asBinary()

        binary[3..12].read {
            readInt()
            val res = readByte()
            assertEquals(7, res)
        }
    }
}