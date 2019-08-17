package hep.dataforge.names

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NameTest {
    @Test
    fun simpleName() {
        val name = "token1.token2.token3".toName()
        assertEquals("token2", name[1].toString())
    }

    @Test
    fun equalityTest() {
        val name1 = "token1.token2[2].token3".toName()
        val name2 = "token1".toName() + "token2[2].token3"
        assertEquals(name1, name2)
    }

    @Test
    fun comparisonTest(){
        val name1 = "token1.token2.token3".toName()
        val name2 = "token1.token2".toName()
        val name3 = "token3".toName()
        assertTrue { name1.startsWith(name2) }
        assertTrue { name1.endsWith(name3) }
        assertFalse { name1.startsWith(name3) }
    }

    @Test
    fun escapeTest(){
        val escapedName = "token\\.one.token2".toName()
        val unescapedName = "token\\.one.token2".asName()

        assertEquals(2, escapedName.length)
        assertEquals(1, unescapedName.length)
        assertEquals(escapedName, escapedName.toString().toName())
    }
}