package hep.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecificationTest {
    class TestStyled(target: MutableItemProvider, default: ItemProvider?) : Scheme(target, default) {
        var list by numberList(1, 2, 3)

        companion object : Specification<TestStyled> {
            override fun read(items: ItemProvider): TestStyled = TestStyled(Config(), items)

            override fun write(target: MutableItemProvider, defaultProvider: ItemProvider): TestStyled =
                TestStyled(target, defaultProvider)
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