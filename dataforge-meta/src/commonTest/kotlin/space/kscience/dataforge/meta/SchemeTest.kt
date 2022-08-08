package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DFExperimental
class SchemeTest {
    @Test
    fun testSchemeWrappingBeforeEdit() {
        val config = MutableMeta()
        val scheme = TestScheme.write(config)
        scheme.a = 29
        assertEquals(29, config["a"].int)
    }

    @Test
    fun testSchemeWrappingAfterEdit() {
        val scheme = TestScheme.empty()
        scheme.a = 29
        val config = MutableMeta()
        scheme.retarget(config)
        assertEquals(29, scheme.a)
    }

    @Test
    fun testSchemeSubscription() {
        val scheme = TestScheme.empty()
        var flag: Int? = null
        scheme.useProperty(TestScheme::a) { a ->
            flag = a
        }
        scheme.a = 2
        assertEquals(2, flag)
    }

    @Test
    fun testListSubscription(){
        val scheme = TestScheme.empty()
        var value: Value? = null
        scheme.v = ListValue(0.0,0.0,0.0)
        scheme.useProperty(TestScheme::v){
            value = it
        }
        scheme.v = ListValue(1.0, 2.0, 3.0)
        assertNotNull(value)
    }
}