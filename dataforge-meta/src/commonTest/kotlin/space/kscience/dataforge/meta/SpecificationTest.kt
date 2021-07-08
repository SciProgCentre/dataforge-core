package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestScheme : Scheme() {
    var list by numberList(1, 2, 3)

    var a by int()
    var b by string()

    companion object : Specification<TestScheme> {
        override fun empty(): TestScheme = TestScheme()

        override fun read(items: ItemProvider): TestScheme =
            wrap(MetaBuilder(), items)

        override fun write(target: MutableItemProvider, defaultProvider: ItemProvider): TestScheme =
            wrap(target, defaultProvider)

    }
}

class SpecificationTest {

//    @Test
//    fun testMetaScheme(){
//        val styled = Meta {
//            repeat(10){
//                "b.a[$it]" put {
//                    "d" put it
//                }
//            }
//        }.asScheme()
//
//        val meta = styled.toMeta()
//
//        assertEquals(10, meta.valueSequence().count())
//
//        val bNode = styled["b"].node
//
//        val aNodes = bNode?.getIndexed("a")
//
//        val allNodes = meta.getIndexed("b.a")
//
//        assertEquals(3, aNodes?.get("3").node["d"].int)
//        assertEquals(3, allNodes["3"].node["d"].int)
//    }


    @Test
    fun testSpecific() {
        val testObject = TestScheme {
            list = emptyList()
        }
        assertEquals(emptyList(), testObject.list)
    }

    @Test
    fun testChildModification() {
        val config = MetaBuilder()
        val child = config.getChild("child")
        val scheme = TestScheme.write(child)
        scheme.a = 22
        scheme.b = "test"
        assertEquals(22, config["child.a"].int)
        assertEquals("test", config["child.b"].string)
    }

    @Test
    fun testChildUpdate() {
        val config = MetaBuilder()
        val child = config.getChild("child")
        child.update(TestScheme) {
            a = 22
            b = "test"
        }
        assertEquals(22, config["child.a"].int)
        assertEquals("test", config["child.b"].string)
    }
}