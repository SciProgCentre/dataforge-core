package hep.dataforge.meta

import hep.dataforge.meta.descriptors.NodeDescriptor
import kotlinx.serialization.json.int
import kotlinx.serialization.json.json
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonMetaTest {
    val json = json {
        "firstValue" to "a"
        "secondValue" to "b"
        "array" to jsonArray {
            +"1"
            +"2"
            +"3"
        }
        "nodeArray" to jsonArray {
            +json {
                "index" to "1"
                "value" to 2
            }
            +json {
                "index" to "2"
                "value" to 3
            }
            +json {
                "index" to "3"
                "value" to 4
            }
        }
    }

    val descriptor = NodeDescriptor{
        node("nodeArray"){
            indexKey = "index"
        }
    }

    @Test
    fun jsonMetaConversion() {
        println(json)
        val meta = json.toMeta(descriptor)
        //println(meta)
        val reconstructed = meta.toJson(descriptor)
        println(reconstructed)
        assertEquals(2, reconstructed["nodeArray"]?.jsonArray?.get(1)?.jsonObject?.get("index")?.int)
        assertEquals(json,reconstructed)
    }
}