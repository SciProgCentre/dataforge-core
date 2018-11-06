package hep.dataforge.meta.io

import hep.dataforge.meta.buildMeta
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaFormatTest{
    @Test
    fun testBinaryMetaFormat(){
        val meta = buildMeta {
            "a" to 22
            "node" to {
                "b" to "DDD"
                "c" to 11.1
            }
        }
        val string = BinaryMetaFormat.stringify(meta)
        val result = BinaryMetaFormat.parse(string)
        assertEquals(meta,result)
    }

    @Test
    fun testJsonMetaFormat(){
        val meta = buildMeta {
            "a" to 22
            "node" to {
                "b" to "DDD"
                "c" to 11.1
            }
        }
        val string = JSONMetaFormat.stringify(meta)
        val result = JSONMetaFormat.parse(string)
        assertEquals(meta,result)
    }

}