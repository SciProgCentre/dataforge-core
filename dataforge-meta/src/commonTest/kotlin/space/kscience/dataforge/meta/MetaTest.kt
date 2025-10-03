package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun reset() {
        val oldMeta = MutableMeta {
            "a" put {
                "value" put "aValue"
            }
            "b" put {
                "value" put "bValue"
            }
            "c" put {
                "value" put "cValue"
            }
        }
        val newMeta = Meta {
            "a" put {
                "value" put "aValue"
            }
            "b" put {
                "value" put "bValue"
            }
            "d" put {
                "value" put "dValue"
            }
        }
        oldMeta.reset(newMeta)
        println(oldMeta)
        assertEquals(setOf("a", "b", "d"), oldMeta.items.keys.map { it.toString() }.toSet())
    }

    @Test
    fun testValueParseQuotedString() {
        assertEquals(StringValue("abc"), Value.parse("\"abc\""))
        assertEquals(StringValue(""), Value.parse("\"\""))
        assertEquals(StringValue("\""), Value.parse("\""))
    }

    @Test
    fun `numeric equality implies equal hash codes`() {
        val intOne: Value = Value.of(1)
        val doubleOne: Value = Value.of(1.0)

        assertEquals(intOne, doubleOne, "1 and 1.0 must be equal as Value")

        assertEquals(
            intOne.hashCode(),
            doubleOne.hashCode(),
            "Equal numeric Values must produce equal hash codes"
        )

        val set = hashSetOf(intOne)
        assertTrue(doubleOne in set, "HashSet containment must work for equal numeric Values")
    }

    @Test
    fun `minus zero and plus zero must hash equally when equal`() {
        val a = Value.of(-0.0)
        val b = Value.of(0.0)

        assertEquals(a, b, "(-0.0) and (+0.0) must be equal as Value")

        assertEquals(a.hashCode(), b.hashCode(), "Equal numeric Values must produce equal hash codes")
    }

}