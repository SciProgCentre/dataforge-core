package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableMetaTest{
    @Test
    fun remove(){
        val meta = MutableMeta {
            "aNode" put {
                "innerNode" put {
                    "innerValue" put true
                }
                "b" put 22
                "c" put "StringValue"
            }
        }

        meta.remove("aNode.c")
        assertEquals(meta["aNode.c"], null)
    }
}