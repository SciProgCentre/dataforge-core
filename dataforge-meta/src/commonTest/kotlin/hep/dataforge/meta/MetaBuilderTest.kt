package hep.dataforge.meta

import hep.dataforge.values.asValue
import kotlin.test.Test
import kotlin.test.assertEquals


class MetaBuilderTest {
    @Test
    fun testBuilder() {
        val meta = buildMeta {
            "a" to 22
            "b" to listOf(1, 2, 3)
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

    @Test
    fun testSNS(){
        val meta = buildMeta {
            repeat(10){
                "b.a[$it]" to it
            }
        }.seal()
        assertEquals(10, meta.asValueSequence().count())

        val nodes = meta.getAll("b.a")

        assertEquals(3, nodes["3"]?.int)
    }

}