package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SchemeTest {
    @Test
    fun testSchemeWrappingBeforeEdit(){
        val config = Config()
        val scheme = TestScheme.wrap(config)
        scheme.a = 29
        assertEquals(29, config["a"].int)
    }

    @Test
    fun testSchemeWrappingAfterEdit(){
        val scheme = TestScheme.empty()
        scheme.a = 29
        val config = Config()
        scheme.retarget(config)
        assertEquals(29, scheme.a)
    }

    @Test
    fun testSchemeSubscription(){
        val scheme = TestScheme.empty()
        var flag: Int? = null
        scheme.onChange(TestScheme::a){a->
            flag = a
        }
        scheme.a = 2
        assertEquals(2, flag)
    }
}