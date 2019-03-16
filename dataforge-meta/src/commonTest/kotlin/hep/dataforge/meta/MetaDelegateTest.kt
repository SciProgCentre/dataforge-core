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
        val testObject = object : Specification {
            override val config: Config = Config()
            var myValue by config.string()
            var safeValue by config.number(2.2)
            var enumValue by config.enum(TestEnum.YES)
        }
        testObject.config["myValue"] = "theString"
        testObject.enumValue = TestEnum.NO
        assertEquals("theString", testObject.myValue)
        assertEquals(TestEnum.NO, testObject.enumValue)
        assertEquals(2.2, testObject.safeValue)
    }

}