package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class DynamicMetaTest {

    @Test
    fun testDynamicMeta() {
        val d = js("{}")
        d.a = 22
        d.array = arrayOf(1,2,3)
        d.b = "myString"
        d.ob = js("{}")
        d.ob.childNode = 18
        d.ob.booleanNode = true

        val meta = DynamicMeta(d)
        assertEquals(true, meta["ob.booleanNode"].boolean)
        assertEquals(2,meta["array[1]"].int)
    }

}