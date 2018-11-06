package hep.dataforge.names

import kotlin.test.Test
import kotlin.test.assertEquals

class NameTest{
    @Test
    fun simpleName(){
        val name = "token1.token2.token3".toName()
        assertEquals("token2", name[1].toString())
    }

    @Test
    fun equalityTest(){
        val name1 = "token1.token2[2].token3".toName()
        val name2 = "token1".toName() + "token2[2].token3"
        assertEquals(name1,name2)
    }
}