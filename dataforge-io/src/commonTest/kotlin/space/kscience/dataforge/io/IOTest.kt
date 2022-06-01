package space.kscience.dataforge.io

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.readUTF8Line
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class IOTest {
    @Test
    fun readBytes() {
        val bytes = ByteArray(8) { it.toByte() }
        val input = ByteReadPacket(bytes)
        @Suppress("UNUSED_VARIABLE") val first = input.readBytes(4)
        val second = input.readBytes(4)
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
                val read = readBytesWithSeparatorTo(this, "---".encodeToByteArray(),  skipUntilEndOfLine = true)
                assertEquals(12, read)
            }
            assertEquals("""
                aaa
                bbb
            """.trimIndent(),array.decodeToString().trim())
            assertEquals("ccc", readUTF8Line()?.trim())
        }

        assertFails {
            binary.read {
                discardWithSeparator("---".encodeToByteArray(), atMost = 3)
            }
        }

        assertFails {
            binary.read{
                discardWithSeparator("-+-".encodeToByteArray())
            }
        }

    }
}