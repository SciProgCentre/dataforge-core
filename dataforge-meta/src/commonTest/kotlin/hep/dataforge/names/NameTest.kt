package hep.dataforge.names

import kotlin.test.*

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
}