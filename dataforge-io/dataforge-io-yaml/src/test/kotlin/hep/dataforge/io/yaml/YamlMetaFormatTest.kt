package hep.dataforge.io.yaml

import hep.dataforge.io.parse
import hep.dataforge.io.toString
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.seal
import kotlin.test.Test
import kotlin.test.assertEquals


class YamlMetaFormatTest {
    @Test
    fun testYamlMetaFormat() {
        val meta = Meta {
            "a" put 22
            "node" put {
                "b" put "DDD"
                "c" put 11.1
                "d" put {
                    "d1" put {
                        "d11" put "aaa"
                        "d12" put "bbb"
                    }
                    "d2" put 2
                }
                "array" put doubleArrayOf(1.0, 2.0, 3.0)
            }
        }
        val string = meta.toString(YamlMetaFormat)
        println(string)
        val result = YamlMetaFormat.parse(string)

        assertEquals<Meta>(meta, meta.seal())

        meta.items.keys.forEach {
            assertEquals(meta[it],result[it],"${meta[it]} != ${result[it]}")
        }

        assertEquals(meta, result)
    }
}