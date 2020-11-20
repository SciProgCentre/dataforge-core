package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecificationTest {
    class TestStyled : Scheme() {
        var list by numberList(1, 2, 3)

        companion object : Specification<TestStyled> {
            override fun read(meta: Meta, defaultProvider: ItemProvider): TestStyled =
                TestStyled().apply {
                    this.config = meta.asConfig()

                }
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