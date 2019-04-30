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

        class InnerSpec(override val config: Config) : Specific {
            var innerValue by string()
        }

        val innerSpec = specification(::InnerSpec)

        val testObject = object : Specific {
            override val config: Config = Config()
            var myValue by string()
            var safeValue by double(2.2)
            var enumValue by config.enum(TestEnum.YES)
            var inner by spec(innerSpec)
        }
        testObject.config["myValue"] = "theString"
        testObject.enumValue = TestEnum.NO

        testObject.inner = innerSpec.build { innerValue = "ddd"}

        assertEquals("theString", testObject.myValue)
        assertEquals(TestEnum.NO, testObject.enumValue)
        assertEquals(2.2, testObject.safeValue)
        assertEquals("ddd", testObject.inner?.innerValue)

    }

}