package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class StyledTest{
    @Test
    fun testSNS(){
        val meta = buildMeta {
            repeat(10){
                "b.a[$it]" put {
                    "d" put it
                }
            }
        }.seal().withStyle()
        assertEquals(10, meta.values().count())

        val bNode = meta["b"].node

        val aNodes = bNode?.getIndexed("a")

        val allNodes = meta.getIndexed("b.a")

        assertEquals(3, aNodes?.get("3").node["d"].int)
        assertEquals(3, allNodes["3"].node["d"].int)
    }

}