package space.kscience.dataforge.io

import kotlinx.io.buffered
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.readByteArray
import kotlinx.io.readLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class IOTest {
    @Test
    fun readBytes() {
        val bytes = ByteArray(8) { it.toByte() }
        val input = ByteArraySource(bytes).buffered()
        @Suppress("UNUSED_VARIABLE") val first = input.readByteArray(4)
        val second = input.readByteArray(4)
        assertEquals(4.toByte(), second[0])
    }

    @Test
    fun readUntilSeparator() {
        val source = """
            aaa
            bbb
            ---
            ccc
            ddd
        """.trimIndent()

        val binary = source.encodeToByteArray().asBinary()

        binary.read {
            val array = ByteArray {
                val read = readWithSeparatorTo(this, "---".encodeToByteString()) + discardLine()
                assertEquals(12, read)
            }
            assertEquals("""
                aaa
                bbb
            """.trimIndent(),array.decodeToString().trim())
            assertEquals("ccc", readLine()?.trim())
        }

        assertFails {
            binary.read {
                discardWithSeparator("---".encodeToByteString(), atMost = 3 )
            }
        }

        assertFails {
            binary.read{
                discardWithSeparator("-+-".encodeToByteString(), errorOnEof = true)
            }
        }

    }
}