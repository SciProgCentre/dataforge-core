package space.kscience.dataforge.values

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ValueEqualityTest {
    @Test
    fun numberValueNotEquals(){
        val a = 0.33.asValue()
        val b = 0.34.asValue()

        println(a.number == b.number)

        assertNotEquals(a,b)
    }

    @Test
    fun arrayEqualsList() {
        val v1 = doubleArrayOf(1.0, 2.0, 3.0).asValue()
        val v2 = listOf(1, 2, 3).map { it.asValue() }.asValue()
        assertEquals(v1, v2)
    }

    @Test
    fun notEquals() {
        val v1 = doubleArrayOf(1.0, 2.0, 3.0).asValue()
        val v2 = listOf(1, 2, 6).map { it.asValue() }.asValue()
        assertNotEquals(v1, v2)
    }

}