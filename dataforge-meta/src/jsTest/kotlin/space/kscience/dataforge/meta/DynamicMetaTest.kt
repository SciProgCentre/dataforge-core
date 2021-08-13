package space.kscience.dataforge.meta

import space.kscience.dataforge.values.ListValue
import space.kscience.dataforge.values.int
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class DynamicMetaTest {

    @Test
    fun testDynamicMeta() {
        val d = js("{}")
        d.a = 22
        d.array = arrayOf(1, 2, 3)
        d.b = "myString"
        d.ob = js("{}")
        d.ob.childNode = 18
        d.ob.booleanNode = true

        val meta = DynamicMeta(d)
        println(meta)
        assertEquals(true, meta["ob.booleanNode"].boolean)
        assertEquals(2, meta["array"]?.value?.list?.get(1)?.int)
        assertEquals(4, meta.items.size)
    }

    @Test
    fun testMetaToDynamic(){
        val meta = Meta {
            "a" put 22
            "array" put ListValue(1, 2, 3)
            "b" put "myString"
            "ob" put {
                "childNode" put 18
                "booleanNode" put true
            }
        }

        val dynamic = meta.toDynamic()
        println(JSON.stringify(dynamic))
        assertEquals(2,dynamic.array[1])
        assertEquals(22, dynamic.a)
        val keys = js("Object.keys(dynamic)") as Array<String>
        assertTrue { keys.contains("ob") }
        assertEquals(18, dynamic.ob.childNode)
        assertEquals<Meta>(meta, DynamicMeta(dynamic))
    }

}