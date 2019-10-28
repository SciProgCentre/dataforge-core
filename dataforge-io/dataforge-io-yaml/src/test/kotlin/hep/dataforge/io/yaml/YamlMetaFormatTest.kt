package hep.dataforge.io.yaml

import hep.dataforge.io.parse
import hep.dataforge.io.toString
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.meta.get
import hep.dataforge.meta.seal
import org.junit.Test
import kotlin.test.assertEquals


class YamlMetaFormatTest{
    @Test
    fun testYamlMetaFormat(){
        val meta = buildMeta {
            "a" to 22
            "node" to {
                "b" to "DDD"
                "c" to 11.1
                "d" to {
                    "d1" to {
                        "d11" to "aaa"
                        "d12" to "bbb"
                    }
                    "d2" to 2
                }
                "array" to doubleArrayOf(1.0, 2.0, 3.0)
            }
        }
        val string = meta.toString(YamlMetaFormat)
        println(string)
        val result = YamlMetaFormat.parse(string)

        assertEquals<Meta>(meta, meta.seal())

        meta.items.keys.forEach {
            if (meta[it] != result[it]) error("${meta[it]} != ${result[it]}")
        }

        assertEquals(meta, result)
    }
}