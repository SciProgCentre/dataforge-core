package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigTest {
    @Test
    fun testIndexedWrite(){
        val config = MutableMeta()
        config["a[1].b"] = 1
        assertEquals(null, config["a.b"].int)
        assertEquals(1, config["a[1].b"].int)
    }
}