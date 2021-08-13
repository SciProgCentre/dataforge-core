package space.kscience.dataforge.properties

import space.kscience.dataforge.meta.Scheme
import space.kscience.dataforge.meta.SchemeSpec
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestScheme : Scheme() {
    var a by int()
    var b by int()
    companion object : SchemeSpec<TestScheme>(::TestScheme)
}

@DFExperimental
class MetaPropertiesTest {
    @Test
    fun testBinding() {
        val scheme = TestScheme.empty()
        val a = scheme.property(TestScheme::a)
        val b = scheme.property(TestScheme::b)
        a.bind(b)
        scheme.a = 2
        assertEquals(2, scheme.b)
        assertEquals(2, b.value)
    }
}