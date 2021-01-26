package hep.dataforge.meta

import hep.dataforge.values.asValue
import kotlin.test.Test
import kotlin.test.assertEquals


class MetaDelegateTest {
    enum class TestEnum {
        YES,
        NO
    }

    class InnerSpec : Scheme() {
        var innerValue by string()

        companion object : SchemeSpec<InnerSpec>(::InnerSpec)
    }

    class TestScheme : Scheme() {
        var myValue by string()
        var safeValue by double(2.2)
        var enumValue by enum(TestEnum.YES)
        var inner by spec(InnerSpec)

        companion object : SchemeSpec<TestScheme>(::TestScheme)
    }

    @Test
    fun delegateTest() {

        val testObject = TestScheme.empty()
        testObject.set("myValue","theString".asValue())
        testObject.enumValue = TestEnum.NO

        testObject.inner = InnerSpec { innerValue = "ddd" }

        assertEquals("theString", testObject.myValue)
        assertEquals(TestEnum.NO, testObject.enumValue)
        assertEquals(2.2, testObject.safeValue)
        assertEquals("ddd", testObject.inner.innerValue)

    }

}