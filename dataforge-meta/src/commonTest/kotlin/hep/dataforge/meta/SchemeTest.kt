package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class SchemeTest{
    @Test
    fun testMetaScheme(){
        val styled = Meta {
            repeat(10){
                "b.a[$it]" put {
                    "d" put it
                }
            }
        }.asScheme()

        val meta = styled.toMeta()

        assertEquals(10, meta.values().count())

        val bNode = styled.getProperty("b").node

        val aNodes = bNode?.getIndexed("a")

        val allNodes = meta.getIndexed("b.a")

        assertEquals(3, aNodes?.get("3").node["d"].int)
        assertEquals(3, allNodes["3"].node["d"].int)
    }

}