package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals


class MetaDelegateTest {
    enum class TestEnum {
        YES,
        NO
    }

    @Test
    fun delegateTest() {

        class InnerSpec : Scheme() {
            var innerValue by string()
        }

        val innerSpec = object : SchemeSpec<InnerSpec>(::InnerSpec){}

        val testObject = object : Scheme(Config()) {
            var myValue by string()
            var safeValue by double(2.2)
            var enumValue by enum(TestEnum.YES)
            var inner by spec(innerSpec)
        }
        testObject.config["myValue"] = "theString"
        testObject.enumValue = TestEnum.NO

        testObject.inner = innerSpec { innerValue = "ddd" }

        assertEquals("theString", testObject.myValue)
        assertEquals(TestEnum.NO, testObject.enumValue)
        assertEquals(2.2, testObject.safeValue)
        assertEquals("ddd", testObject.inner?.innerValue)

    }

}