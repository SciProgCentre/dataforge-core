package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableMetaTest {
    @Test
    fun remove() {
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
    fun withDefault() {
        val metaWithDefault = MutableMeta().withDefault(
            Meta {
                "a" put {
                    value = 22.asValue()
                    "b" put true
                }
            }
        )

        assertEquals(22, metaWithDefault["a"].int)
        assertEquals(true, metaWithDefault.getValue("a.b")?.boolean)

        metaWithDefault["a.b"] = "false"

        assertEquals(false, metaWithDefault["a.b"]?.boolean)
        assertEquals(false, metaWithDefault.getValue("a.b")?.boolean)
        assertEquals(22, metaWithDefault.getValue("a")?.int)

    }
}