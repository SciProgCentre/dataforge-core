package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

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

    @Test
    fun recursiveMeta(){
        val meta = MutableMeta {
            "a" put 2
        }

        assertFails { meta["child.a"] = meta}
    }
}