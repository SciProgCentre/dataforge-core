package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetaSequenceTest {

    @Test
    fun testEmptyMetaValueSequence() {
        val meta = Meta.EMPTY
        val sequence = meta.valueSequence()
        assertTrue(sequence.toList().isEmpty(), "Empty Meta should yield an empty sequence")
    }

    @Test
    fun testRootValueOnlySequence() {
        val value = Value.of(42)
        val meta = Meta(value)
        val list = meta.valueSequence().toList()
        assertEquals(1, list.size)
        assertEquals(Name.EMPTY, list[0].first)
        assertEquals(value, list[0].second)
    }

    @Test
    fun testNestedValueSequence() {
        val meta = Meta {
            "a" put 1
            "b" put {
                "c" put 2
            }
        }
        val list = meta.valueSequence().toList().sortedBy { it.first.toString() }
        assertEquals(2, list.size)
        assertEquals("a".parseAsName(), list[0].first)
        assertEquals(Value.of(1), list[0].second)
        assertEquals("b.c".parseAsName(), list[1].first)
        assertEquals(Value.of(2), list[1].second)
    }

    @Test
    fun testMixedValueSequence() {
        val meta = Meta {
            "root" put "rootValue"
            "nodeWithoutValue" put {
                "leaf" put "leafValue"
            }
            "nodeWithValue" put {
                value = Value.of("nodeValue")
                "child" put "childValue"
            }
        }
        val list = meta.valueSequence().toList().sortedBy { it.first.toString() }
        // Expected:
        // root: rootValue
        // nodeWithoutValue.leaf: leafValue
        // nodeWithValue: nodeValue
        // nodeWithValue.child: childValue
        
        assertEquals(4, list.size)
        val map = list.associate { it.first.toString() to it.second.string }
        assertEquals("rootValue", map["root"])
        assertEquals("leafValue", map["nodeWithoutValue.leaf"])
        assertEquals("nodeValue", map["nodeWithValue"])
        assertEquals("childValue", map["nodeWithValue.child"])
    }

    @Test
    fun testIndexedValueSequence() {
        val meta = Meta {
            "a" putIndexed listOf(
                Meta(Value.of(1)),
                Meta(Value.of(2))
            )
        }
        val list = meta.valueSequence().toList().sortedBy { it.first.toString() }
        assertEquals(2, list.size)
        assertEquals("a[0]".parseAsName(), list[0].first)
        assertEquals(Value.of(1), list[0].second)
        assertEquals("a[1]".parseAsName(), list[1].first)
        assertEquals(Value.of(2), list[1].second)
    }

    @Test
    fun testDeeplyNestedValueSequence() {
        val meta = Meta {
            "level1" put {
                "level2" put {
                    "level3" put {
                        "level4" put 4
                    }
                }
            }
        }
        val list = meta.valueSequence().toList()
        assertEquals(1, list.size)
        assertEquals("level1.level2.level3.level4".parseAsName(), list[0].first)
        assertEquals(Value.of(4), list[0].second)
    }
    
    @Test
    fun testMetaNodeSequence() {
        val meta = Meta {
            "a" put 1
            "b" put {
                "c" put 2
            }
        }
        val list = meta.nodeSequence().toList().sortedBy { it.first.toString() }
        // Expected:
        // a: Meta(1)
        // b: Meta { c put 2 }
        // b.c: Meta(2)
        assertEquals(3, list.size)
        assertEquals("a", list[0].first.toString())
        assertEquals("b", list[1].first.toString())
        assertEquals("b.c", list[2].first.toString())
    }
}
