package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class MetaDelegateTest {
    enum class TestEnum {
        YES,
        NO
    }

    class InnerScheme : Scheme() {
        var innerValue by string()

        companion object : SchemeSpec<InnerScheme>(::InnerScheme)
    }

    class TestScheme : Scheme() {
        var myValue by string()
        var safeValue by double(2.2)
        var enumValue by enum(TestEnum.YES)
        var inner by scheme(InnerScheme)

        companion object : SchemeSpec<TestScheme>(::TestScheme)
    }

    @Test
    fun delegateTest() {

        val testObject = TestScheme.empty()
        testObject.meta["myValue"] = "theString"
        testObject.enumValue = TestEnum.NO

        testObject.inner = InnerScheme { innerValue = "ddd" }

        assertEquals("theString", testObject.myValue)
        assertEquals(TestEnum.NO, testObject.enumValue)
        assertEquals(2.2, testObject.safeValue)
        assertEquals("ddd", testObject.inner.innerValue)

    }

}