package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableMetaTest{
    @Test
    fun testRemove(){
        val meta = buildMeta {
            "aNode" to {
                "innerNode" to {
                    "innerValue" to true
                }
                "b" to 22
                "c" to "StringValue"
            }
        }.toConfig()

        meta.remove("aNode.c")
        assertEquals(meta["aNode.c"], null)
    }
}