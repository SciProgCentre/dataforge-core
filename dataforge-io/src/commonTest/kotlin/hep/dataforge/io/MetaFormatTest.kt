package hep.dataforge.io

import hep.dataforge.meta.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.json
import kotlinx.serialization.json.jsonArray
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

        assertEquals<Meta>(meta, result)
    }

    @Test
    fun testJsonToMeta(){
        val json = jsonArray{
            //top level array
            +jsonArray {
                +JsonPrimitive(88)
                +json{
                    "c" to "aasdad"
                    "d" to true
                }
            }
            +"value"
            +jsonArray {
                +JsonPrimitive(1.0)
                +JsonPrimitive(2.0)
                +JsonPrimitive(3.0)
            }
        }
        val meta = json.toMetaItem().node!!

        assertEquals(true, meta["@value[0].@value[1].d"].boolean)
        assertEquals("value", meta["@value[1]"].string)
        assertEquals(listOf(1.0,2.0,3.0),meta["@value[2"].value?.list?.map{it.number.toDouble()})
    }

}