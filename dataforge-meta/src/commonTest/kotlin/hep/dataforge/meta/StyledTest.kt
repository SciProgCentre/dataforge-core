package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class StyledTest{
    @Test
    fun testSNS(){
        val meta = buildMeta {
            repeat(10){
                "b.a[$it]" to {
                    "d" to it
                }
            }
        }.seal().withStyle()
        assertEquals(10, meta.asValueSequence().count())

        val bNode = meta["b"].node

        val aNodes = bNode?.getAll("a")

        val allNodes = meta.getAll("b.a")

        assertEquals(3, aNodes?.get("3")?.node["d"]?.int)
        assertEquals(3, allNodes["3"]?.node["d"]?.int)
    }

}