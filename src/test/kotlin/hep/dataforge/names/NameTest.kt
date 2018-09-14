package hep.dataforge.names

import kotlin.test.Test
import kotlin.test.assertEquals

class NameTest{
    @Test
    fun simpleName(){
        val name = "token1.token2.token3".toName()
        assertEquals("token2", name[1].toString())
    }
}