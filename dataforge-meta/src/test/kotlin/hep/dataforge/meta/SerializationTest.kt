package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun testJSONSerialization() {
        val meta = buildMeta {
            "a" to 2
            "b" to {
                "c" to "ddd"
                "d" to 2.2
            }
        }
        val json = meta.toJSON()
        println(json)
        val result = json.toMeta()
        assertEquals(meta, result)
    }

//    @Test
//    fun testIndirectSerialization() {
//        val meta = buildMeta {
//            "a" to 2
//            "b" to {
//                "c" to "ddd"
//                "d" to 2.2
//            }
//        }
//        val json = JSON.stringify(meta.toMap())
//        println(json)
////        val result = json.toMeta()
////        assertEquals(meta, result)
//    }

//    @Test
//    fun testWeirdSerialization() {
//        val meta = buildMeta {
//            "a" to 2
//            "b" to {
//                "c" to "ddd"
//                "d" to 2.2
//            }
//        }
//        val json = JSON.stringify(meta.toJSON())
//        println(json)
//        val result: JsonObject = JSON.parse(json)
//        assertEquals(meta, result.toMeta())
//    }
}