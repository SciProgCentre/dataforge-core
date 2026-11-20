package space.kscience.dataforge.io

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ByteArrayBinarySuspendTest {
    @Test
    fun readsEmptyBinary() = runBlocking {
        var calls = 0
        assertTrue(Binary.EMPTY.readSuspend {
            calls++
            exhausted()
        })
        assertEquals(1, calls)
    }

    @Test
    fun readsAtEndOfBinaryAndView() = runBlocking {
        val binary = byteArrayOf(10, 11, 12, 13, 14, 15).asBinary()
        for (view in listOf(binary, binary.view(2, 3), binary.view(binary.size, 0))) {
            for (limit in listOf(0, 10)) {
                var calls = 0
                assertTrue(view.readSuspend(view.size, limit) {
                    calls++
                    exhausted()
                })
                assertEquals(1, calls)
            }
        }
    }

    @Test
    fun readOffsetsAreRelativeToView() = runBlocking {
        val binary = byteArrayOf(10, 11, 12, 13, 14, 15).asBinary().view(2, 3)
        assertContentEquals(byteArrayOf(13, 14), binary.readSuspend(1, Int.MAX_VALUE) { readByteArray() })
        assertTrue(binary.readSuspend(1, 0) { exhausted() })
    }

    @Test
    fun rejectsInvalidReadRangesBeforeCallback() = runBlocking {
        val binary = byteArrayOf(10, 11, 12, 13, 14, 15).asBinary()
        for (view in listOf(binary, binary.view(2, 3), Binary.EMPTY)) {
            for ((offset, limit) in listOf(-1 to 0, view.size + 1 to 0, 0 to -1, view.size to -1)) {
                var calls = 0
                assertFailsWith<IllegalArgumentException> { view.readSuspend(offset, limit) { calls++ } }
                assertEquals(0, calls)
            }
        }
    }

    @Test
    fun propagatesCallbackFailureAndClosesSource() = runBlocking {
        for (failure in listOf(IllegalStateException("reader failure"), CancellationException("reader cancelled"))) {
            lateinit var source: Source
            assertSame(failure, assertFailsWith<Exception> {
                byteArrayOf(1).asBinary().readSuspend {
                    source = this
                    yield()
                    throw failure
                }
            })
            assertFailsWith<IllegalStateException> { source.exhausted() }
        }
    }
}
