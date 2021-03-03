package space.kscience.dataforge.io.yaml

import space.kscience.dataforge.io.parse
import space.kscience.dataforge.io.toString
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.seal
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals


@DFExperimental
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