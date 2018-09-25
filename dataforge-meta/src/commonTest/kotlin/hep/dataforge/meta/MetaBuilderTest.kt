package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class MetaBuilderTest{
    @Test
    fun testBuilder(){
        val meta = buildMeta {
            "a" to 22
            "b" to listOf(1,2,3)
            this["c"] = "myValue".asValue()
            "node" to {
                "e" to 12.2
                "childNode" to {
                    "f" to true
                }
            }
        }
        assertEquals(12.2, meta["node.e"]?.double)
        assertEquals(true, meta["node.childNode.f"]?.boolean)
    }

}