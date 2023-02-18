package space.kscience.dataforge.names

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NameTest {
    @Test
    fun simpleName() {
        val name = Name.parse("token1.token2.token3")
        assertEquals("token2", name[1].toString())
    }

    @Test
    fun equalityTest() {
        val name1 = Name.parse("token1.token2[2].token3")
        val name2 = "token1".asName() + "token2[2].token3"
        assertEquals(name1, name2)
    }

    @Test
    fun comparisonTest() {
        val name1 = Name.parse("token1.token2.token3")
        val name2 = Name.parse("token1.token2")
        val name3 = Name.parse("token3")
        assertTrue { name1.startsWith(name2) }
        assertTrue { name1.endsWith(name3) }
        assertFalse { name1.startsWith(name3) }
    }

    @Test
    fun escapeTest() {
        val escapedName = Name.parse("token\\.one.token2")
        val unescapedName = "token\\.one.token2".asName()

        assertEquals(2, escapedName.length)
        assertEquals(1, unescapedName.length)
        assertEquals(escapedName, Name.parse(escapedName.toString()))
    }

    @Test
    fun cutFirst() {
        val name = Name.parse("a.b.c")
        assertEquals("b.c".parseAsName(), name.cutFirst())
    }

    @Test
    fun cutLast() {
        val name = Name.parse("a.b.c")
        assertEquals("a.b".parseAsName(), name.cutLast())
    }
}