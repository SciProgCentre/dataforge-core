package hep.dataforge.meta

import hep.dataforge.meta.scheme.Scheme
import hep.dataforge.meta.scheme.Specification
import hep.dataforge.meta.scheme.numberList
import hep.dataforge.names.Name
import kotlin.test.Test
import kotlin.test.assertEquals

class SpecificationTest {
    class TestStyled(config: Config, defaultProvider: (Name) -> MetaItem<*>?) :
        Scheme(config, defaultProvider) {
        var list by numberList(1, 2, 3)

        companion object : Specification<TestStyled> {
            override fun wrap(
                config: Config,
                defaultProvider: (Name) -> MetaItem<*>?
            ): TestStyled = TestStyled(config, defaultProvider)
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