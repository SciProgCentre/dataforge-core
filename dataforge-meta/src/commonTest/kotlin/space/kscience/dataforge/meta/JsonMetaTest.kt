package space.kscience.dataforge.meta

import kotlinx.serialization.json.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.node
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonMetaTest {
    val json = buildJsonObject {
        put("firstValue", "a")
        put("secondValue", "b")
        put("array", buildJsonArray {
            add("1")
            add("2")
            add("3")
        })
        put("nodeArray", buildJsonArray {
            add(buildJsonObject {
                put("index", "1")
                put("value", 2)
            })
            add(buildJsonObject {
                put("index", "2")
                put("value", 3)
            })
            add(buildJsonObject {
                put("index", "3")
                put("value", 4)
            })
        })
    }

    val descriptor = MetaDescriptor {
        node("nodeArray") {
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
        assertEquals(
            2,
            reconstructed
                .jsonObject["nodeArray"]
                ?.jsonArray
                ?.get(1)
                ?.jsonObject
                ?.get("index")
                ?.jsonPrimitive
                ?.int
        )
        assertEquals(json, reconstructed)
    }

    @Test
    fun jsonArrayToMetaConversion() {
        val jsonArray = buildJsonArray {
            addJsonArray {
                add(1); add(2); add(3)
            }
            addJsonArray {
                add(4); add(5); add(6)
            }
            addJsonArray {
                add(7); add(8); add(9)
            }
        }

        assertEquals(jsonArray, jsonArray.toMeta().toJson())


        val jsonWithChild = buildJsonObject {
            "child" to jsonArray
        }

        assertEquals(jsonWithChild, jsonWithChild.toMeta().toJson())
    }

    @Test
    fun metaToJsonArrayConversion() {
        val meta = Meta {
            value = listOf(
                listOf(1, 2, 3).asValue(),
                listOf(4, 5, 6).asValue(),
                listOf(7, 8, 9).asValue()
            ).asValue()
        }
        val json = meta.toJson()

        assertEquals(json, json.toMeta().toJson())
    }
}