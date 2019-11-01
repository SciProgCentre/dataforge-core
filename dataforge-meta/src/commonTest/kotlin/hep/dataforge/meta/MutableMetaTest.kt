package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableMetaTest{
    @Test
    fun testRemove(){
        val meta = buildMeta {
            "aNode" put {
                "innerNode" put {
                    "innerValue" put true
                }
                "b" put 22
                "c" put "StringValue"
            }
        }.toConfig()

        meta.remove("aNode.c")
        assertEquals(meta["aNode.c"], null)
    }
}