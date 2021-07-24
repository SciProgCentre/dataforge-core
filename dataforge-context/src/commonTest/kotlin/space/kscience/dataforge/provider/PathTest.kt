package space.kscience.dataforge.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class PathTest {
    @Test
    fun testParse(){
        val nameString = "a.b.c.d"
        val pathString = "a.b/c.d"
        assertEquals(1, Path.parse(nameString).length)
        assertEquals(2, Path.parse(pathString).length)
    }
}