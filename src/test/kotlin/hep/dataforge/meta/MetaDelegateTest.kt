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
        val testObject = object : SimpleConfigurable(Configuration()) {
            var myValue by string()
            var safeValue by number(2.2)
            var enumValue by enum(TestEnum.YES)
        }
        testObject.config["myValue"] = "theString"
        testObject.enumValue = TestEnum.NO
        assertEquals("theString", testObject.myValue)
        assertEquals(TestEnum.NO, testObject.enumValue)
        assertEquals(2.2, testObject.safeValue)
    }

}