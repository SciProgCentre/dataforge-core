package space.kscience.dataforge.io

import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BinaryTest {
    @Test
    fun readsEmptyBinary() {
        var calls = 0
        assertTrue(Binary.EMPTY.read {
            calls++
            exhausted()
        })
        assertEquals(1, calls)
    }

    @Test
    fun readsAtEndOfBinaryAndView() {
        val binary = byteArrayOf(10, 11, 12, 13, 14, 15).asBinary()
        for (view in listOf(binary, binary.view(2, 3), binary.view(binary.size, 0))) {
            for (limit in listOf(0, 10)) {
                var calls = 0
                assertTrue(view.read(view.size, limit) {
                    calls++
                    exhausted()
                })
                assertEquals(1, calls)
            }
        }
    }

    @Test
    fun readOffsetsAreRelativeToView() {
        val binary = byteArrayOf(10, 11, 12, 13, 14, 15).asBinary().view(2, 3)
        assertContentEquals(byteArrayOf(13, 14), binary.read(1, Int.MAX_VALUE) { readByteArray() })
        assertTrue(binary.read(1, 0) { exhausted() })
    }

    @Test
    fun rejectsInvalidReadRangesBeforeCallback() {
        val binary = byteArrayOf(10, 11, 12, 13, 14, 15).asBinary()
        for (view in listOf(binary, binary.view(2, 3), Binary.EMPTY)) {
            for ((offset, limit) in listOf(-1 to 0, view.size + 1 to 0, 0 to -1, view.size to -1)) {
                var calls = 0
                assertFailsWith<IllegalArgumentException> { view.read(offset, limit) { calls++ } }
                assertEquals(0, calls)
            }
        }
    }

    @Test
    fun propagatesCallbackFailureAndClosesSource() {
        val failure = IllegalStateException("reader failure")
        lateinit var source: Source
        assertSame(failure, assertFailsWith<IllegalStateException> {
            byteArrayOf(1).asBinary().read {
                source = this
                throw failure
            }
        })
        assertFailsWith<IllegalStateException> { source.exhausted() }
    }

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
