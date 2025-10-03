package space.kscience.dataforge.io

import kotlinx.io.Buffer
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

    @Test
    fun testReadWithSeparatorToLosesByteOnShortEof() {
        val separator = "END_SEPARATOR".encodeToByteString()
        val content = "short"
        val source = Buffer().apply { write(content.encodeToByteArray()) }
        val output = Buffer()

        source.readWithSeparatorTo(output, separator, errorOnEof = false)

        assertEquals(content, output.readByteArray().decodeToString(), "Should not lose the first byte on short EOF")
    }

    @Test
    fun `range operator on Binary is inclusive`() {
        val src = byteArrayOf(0, 1, 2, 3, 4).asBinary()

        val slice = src[1..3]

        assertEquals(3, slice.size, "Binary[1..3] must contain 3 bytes")

        val bytes = slice.toByteArray()
        assertEquals(listOf<Byte>(1, 2, 3), bytes.toList(), "Slice content must include the right bound")
    }
}