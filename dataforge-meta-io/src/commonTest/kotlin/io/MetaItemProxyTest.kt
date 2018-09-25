package io

import hep.dataforge.meta.buildMeta
import hep.dataforge.meta.io.MetaItemProxy
import hep.dataforge.meta.io.toMap
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import kotlin.test.Test

class MetaItemProxyTest {
    @Test
    fun testGeneration() {
        MetaItemProxy::class.serializer()
    }


    @Test
    fun testProxySerialization() {
        val meta = buildMeta {
            "a" to 2
            "b" to {
                "c" to "ddd"
                "d" to 2.2
            }
        }
        val json = JSON.indented.stringify(meta.toMap())
        println(json)
//        val result: Map<String, MetaItemProxy> = JSON.parse(json)
//        assertEquals(meta,result.to)
    }

//    @Test
//    fun testJSONSerialization() {
//        val meta = buildMeta {
//            "a" to 2
//            "b" to {
//                "c" to "ddd"
//                "d" to 2.2
//            }
//        }
//        val json = meta.toJSON()
//        println(json)
//        val result = json.toMeta()
//        assertEquals(meta, result)
//    }

}