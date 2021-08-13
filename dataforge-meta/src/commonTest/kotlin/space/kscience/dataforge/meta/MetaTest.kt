package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.values.NumberValue
import space.kscience.dataforge.values.True
import space.kscience.dataforge.values.Value
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaTest {
    @Test
    fun valueEqualityTest() {
        assertEquals(NumberValue(22), NumberValue(22))
        assertEquals(NumberValue(22.0), NumberValue(22))
        assertEquals(True, Value.of(true))
    }

    @Test
    fun metaEqualityTest() {
        val meta1 = Meta {
            "a" put 22
            "b" put {
                "c" put "ddd"
            }
        }
        val meta2 = Meta {
            "b" put {
                "c" put "ddd"
            }
            "a" put 22
        }.seal()
        assertEquals<Meta>(meta1, meta2)
    }

    @OptIn(DFExperimental::class)
    @Test
    fun metaToMap() {
        val meta = Meta {
            "a" put 22
            "b" put {
                "c" put "ddd"
            }
            "list" putIndexed (0..4).map {
                Meta {
                    "value" put it
                }
            }
        }
        val map = meta.toMap()
        val reconstructed = map.toMeta()

        assertEquals(meta, reconstructed)
    }

    @Test
    fun indexed() {
        val meta = Meta {
            (0..20).forEach {
                set("a[$it]", it)
            }
        }
        val indexed = meta.getIndexed("a[1.]")
        assertEquals(10, indexed.size)
        assertEquals(null, indexed["8"])
        assertEquals(12, indexed["12"].int)
    }
}