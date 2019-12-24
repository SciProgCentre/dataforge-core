package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecificationTest {
    class TestSpecific(override val config: Config) : Specific {
        var list by numberList(1, 2, 3)

        companion object : Specification<TestSpecific> {
            override fun wrap(config: Config): TestSpecific = TestSpecific(config)
        }
    }


    @Test
    fun testSpecific(){
        val testObject = TestSpecific {
            list = emptyList()
        }
        assertEquals(emptyList(), testObject.list)
    }
}