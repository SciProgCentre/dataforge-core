package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecificationTest {
    class TestStyled(config: Config, default: ItemProvider?) : Scheme(config, default) {
        var list by numberList(1, 2, 3)

        companion object : Specification<TestStyled> {
            override fun read(items: ItemProvider): TestStyled = TestStyled(Config(), items)

            override fun write(config: Config, defaultProvider: ItemProvider): TestStyled =
                TestStyled(config, defaultProvider)
        }
    }


    @Test
    fun testSpecific() {
        val testObject = TestStyled {
            list = emptyList()
        }
        assertEquals(emptyList(), testObject.list)
    }
}