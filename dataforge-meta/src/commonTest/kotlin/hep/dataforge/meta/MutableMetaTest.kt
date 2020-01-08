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
        }.asConfig()

        meta.remove("aNode.c")
        assertEquals(meta["aNode.c"], null)
    }
}