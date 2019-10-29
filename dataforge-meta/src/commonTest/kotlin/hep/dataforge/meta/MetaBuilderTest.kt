package hep.dataforge.meta

import hep.dataforge.values.asValue
import kotlin.test.Test
import kotlin.test.assertEquals


class MetaBuilderTest {
    @Test
    fun testBuilder() {
        val meta = buildMeta {
            "a" put 22
            "b" put listOf(1, 2, 3)
            this["c"] = "myValue".asValue()
            "node" put {
                "e" put 12.2
                "childNode" put {
                    "f" put true
                }
            }
        }
        assertEquals(12.2, meta["node.e"]?.double)
        assertEquals(true, meta["node.childNode.f"]?.boolean)
    }

    @Test
    fun testSNS(){
        val meta = buildMeta {
            repeat(10){
                "b.a[$it]" put it
            }
        }.seal()
        assertEquals(10, meta.values().count())

        val nodes = meta.getIndexed("b.a")

        assertEquals(3, nodes["3"]?.int)
    }

}