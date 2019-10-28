package hep.dataforge.meta

import hep.dataforge.values.NumberValue
import hep.dataforge.values.True
import hep.dataforge.values.Value
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
        val meta1 = buildMeta {
            "a" to 22
            "b" to {
                "c" to "ddd"
            }
        }
        val meta2 = buildMeta {
            "b" to {
                "c" to "ddd"
            }
            "a" to 22
        }.seal()
        assertEquals<Meta>(meta1, meta2)
    }

    @Test
    fun metaToMap(){
        val meta = buildMeta {
            "a" to 22
            "b" to {
                "c" to "ddd"
            }
            "list" to (0..4).map {
                buildMeta {
                    "value" to it
                }
            }
        }
        val map = meta.toMap()
        val reconstructed = map.toMeta()

        assertEquals(meta,reconstructed)
    }
}