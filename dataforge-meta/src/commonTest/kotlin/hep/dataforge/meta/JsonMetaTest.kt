package hep.dataforge.meta

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
                "index" to 1
            }
            +json {
                "index" to 2
            }
            +json {
                "index" to 3
            }
        }
    }

    @Test
    fun jsonMetaConversion() {
        val meta = json.toMeta()
        val reconstructed = meta.toJson()
        println(json)
        println(reconstructed)
        assertEquals(2, reconstructed["nodeArray"]?.jsonArray?.get(1)?.jsonObject?.get("index")?.int)
    }
}