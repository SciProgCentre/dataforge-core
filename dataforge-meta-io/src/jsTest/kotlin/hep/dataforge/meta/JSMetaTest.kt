package hep.dataforge.meta

import hep.dataforge.meta.io.JSMeta
import kotlin.js.json
import kotlin.test.Test
import kotlin.test.assertEquals

class JSMetaTest{
    @Test
    fun testConverstion(){
        val test = json(
                "a" to 2,
                "b" to "ddd"
        )
        val meta = JSMeta(test)
        assertEquals(2, meta["a"]!!.int)
    }
}