package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecificationTest {
    class TestStyled(config: Config, defaultProvider: ItemProvider) :
        Scheme(config, defaultProvider) {
        var list by numberList(1, 2, 3)

        companion object : Specification<TestStyled> {
            override fun wrap(meta: Meta, defaultProvider: ItemProvider): TestStyled =
                TestStyled(meta.asConfig(), defaultProvider)
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