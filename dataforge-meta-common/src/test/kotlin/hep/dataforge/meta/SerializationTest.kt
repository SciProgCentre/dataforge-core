package scientifik.kplot.remote

import hep.dataforge.meta.buildMeta
import hep.dataforge.meta.get
import hep.dataforge.meta.value
import kotlinx.serialization.json.JSON
import kotlin.test.Test

class SerializationTest{
    @Test
    fun testMetaSerialization(){
        val meta = buildMeta {
            "a" to 2
            "b" to {
                "c" to "ddd"
                "d" to 2.2
            }
        }
        val json = JSON.stringify(meta["a"]?.value!!)
        println(json)
    }
}