package hep.dataforge.meta.io

import hep.dataforge.meta.buildMeta
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaFormatTest {
    @Test
    fun testBinaryMetaFormat() {
        val meta = buildMeta {
            "a" to 22
            "node" to {
                "b" to "DDD"
                "c" to 11.1
            }
        }
        val string = meta.asString(BinaryMetaFormat)
        val result = BinaryMetaFormat.parse(string)
        assertEquals(meta, result)
    }

    @Test
    fun testJsonMetaFormat() {
        val meta = buildMeta {
            "a" to 22
            "node" to {
                "b" to "DDD"
                "c" to 11.1
            }
        }
        val string = meta.asString(JsonMetaFormat)
        val result = JsonMetaFormat.parse(string)
        assertEquals(meta, result)
    }

}