package hep.dataforge.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.meta.get
import hep.dataforge.meta.seal
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
                "array" to doubleArrayOf(1.0, 2.0, 3.0)
            }
        }
        val bytes = meta.toBytes(BinaryMetaFormat)
        val result = BinaryMetaFormat.fromBytes(bytes)
        assertEquals(meta, result)
    }

    @Test
    fun testJsonMetaFormat() {
        val meta = buildMeta {
            "a" to 22
            "node" to {
                "b" to "DDD"
                "c" to 11.1
                "array" to doubleArrayOf(1.0, 2.0, 3.0)
            }
        }
        val string = meta.toString(JsonMetaFormat)
        val result = JsonMetaFormat.parse(string)

        assertEquals<Meta>(meta, meta.seal())

        meta.items.keys.forEach {
            if (meta[it] != result[it]) error("${meta[it]} != ${result[it]}")
        }

        assertEquals(meta, result)
    }

}