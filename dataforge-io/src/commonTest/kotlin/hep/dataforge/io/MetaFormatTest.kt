package hep.dataforge.io

import hep.dataforge.meta.*
import hep.dataforge.meta.JsonMeta.Companion.JSON_ARRAY_KEY
import kotlinx.io.asBinary
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

fun Meta.toByteArray(format: MetaFormat = JsonMetaFormat) = buildByteArray {
    format.run { writeObject(this@toByteArray) }
}

fun MetaFormat.fromByteArray(packet: ByteArray): Meta {
    return packet.asBinary().read { readObject() }
}

class MetaFormatTest {
    @Test
    fun testBinaryMetaFormat() {
        val meta = Meta {
            "a" put 22
            "node" put {
                "b" put "DDD"
                "c" put 11.1
                "array" put doubleArrayOf(1.0, 2.0, 3.0)
            }
        }
        val bytes = meta.toByteArray(BinaryMetaFormat)
        val result = BinaryMetaFormat.fromByteArray(bytes)
        assertEquals(meta, result)
    }

    @Test
    fun testJsonMetaFormat() {
        val meta = Meta {
            "a" put 22
            "node" put {
                "b" put "DDD"
                "c" put 11.1
                "array" put doubleArrayOf(1.0, 2.0, 3.0)
            }
        }
        val string = meta.toString(JsonMetaFormat)
        val result = JsonMetaFormat.parse(string)

        assertEquals<Meta>(meta, meta.seal())

        meta.items.keys.forEach {
            if (meta[it] != result[it]) error("${meta[it]} != ${result[it]}")
        }

        assertEquals<Meta>(meta, result)
    }

    @Test
    fun testJsonToMeta() {
        val json = buildJsonArray {
            //top level array
            add(buildJsonArray {
                add(JsonPrimitive(88))
                add(buildJsonObject {
                    put("c", "aasdad")
                    put("d", true)
                })
            })
            add("value")
            add(buildJsonArray {
                add(JsonPrimitive(1.0))
                add(JsonPrimitive(2.0))
                add(JsonPrimitive(3.0))
            })
        }
        val meta = json.toMetaItem().node!!

        assertEquals(true, meta["$JSON_ARRAY_KEY[0].$JSON_ARRAY_KEY[1].d"].boolean)
        assertEquals("value", meta["$JSON_ARRAY_KEY[1]"].string)
        assertEquals(listOf(1.0, 2.0, 3.0), meta["$JSON_ARRAY_KEY[2"].value?.list?.map { it.number.toDouble() })
    }

}