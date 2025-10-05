package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnsignedAccessorTest {

    @Test
    fun `test strict uint accessor`() {
        assertEquals(255U, NumberValue(255).uint)
        assertEquals(42U, StringValue("42").uint)
        // Strict accessor MUST return null for out-of-range and negative values
        assertNull(NumberValue(UInt.MAX_VALUE.toLong() + 1).uint)
        assertNull(NumberValue(-1).uint)
    }

    @Test
    fun `test strict ulong accessor`() {
        assertEquals(255UL, NumberValue(255L).ulong)
        assertEquals(42UL, StringValue("42").ulong)
        // Strict accessor MUST return null for negative values
        assertNull(NumberValue(-1L).ulong)
        // Should correctly read ULong from a string representation
        assertEquals(ULong.MAX_VALUE, StringValue(ULong.MAX_VALUE.toString()).ulong)
    }

    private class UnsignedScheme : Scheme() {
        var uint: UInt by uint(0U)
        var ulong: ULong by ulong(0UL)
        var largeUlong: ULong by ulong(0UL)

        companion object : SchemeSpec<UnsignedScheme>(::UnsignedScheme)
    }

    @Test
    fun `test delegates storage policy`() {
        val scheme = UnsignedScheme()

        // Write UInt, should be stored as NumberValue(Long)
        scheme.uint = 123U
        assertTrue(scheme.meta["uint"]?.value is NumberValue)
        assertEquals(123L, scheme.meta["uint"]?.long)

        // Write small ULong, should be stored as NumberValue(Long)
        scheme.ulong = 456UL
        assertTrue(scheme.meta["ulong"]?.value is NumberValue)
        assertEquals(456L, scheme.meta["ulong"]?.long)

        // Write large ULong, should be stored as StringValue
        scheme.largeUlong = ULong.MAX_VALUE
        assertTrue(scheme.meta["largeUlong"]?.value is StringValue)
        assertEquals(ULong.MAX_VALUE.toString(), scheme.meta["largeUlong"]?.string)
    }

    @Test
    fun `test delegate round-trip`() {
        val scheme = UnsignedScheme()

        val uintVal = UInt.MAX_VALUE
        val ulongVal = 12345UL
        val largeUlongVal = ULong.MAX_VALUE

        scheme.uint = uintVal
        scheme.ulong = ulongVal
        scheme.largeUlong = largeUlongVal

        assertEquals(uintVal, scheme.uint)
        assertEquals(ulongVal, scheme.ulong)
        assertEquals(largeUlongVal, scheme.largeUlong)
    }

    @OptIn(DFExperimental::class)
    @Test
    fun `test delegate strictness on read`() {
        val meta = MutableMeta {
            "uint" put -1
            "ulong" put -1L
        }
        val scheme = UnsignedScheme()
        scheme.retarget(meta)

        // Reading should fail (return null) and default value should be used
        assertEquals(0U, scheme.uint)
        assertEquals(0UL, scheme.ulong)
    }
}