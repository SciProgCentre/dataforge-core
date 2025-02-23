package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


internal class SubScheme : Scheme() {

    var subValue by string()

    companion object : SchemeSpec<SubScheme>(::SubScheme)
}

internal class TestScheme : Scheme() {
    var list by numberList(1, 2, 3)

    var a by int()
    var b by string()

    var v by value()

    var sub by scheme(SubScheme)

    companion object : SchemeSpec<TestScheme>(::TestScheme)
}

private class SchemeWithInit : Scheme() {
    init {
        set("initial", "initialValue")
    }

    var initial by string()

    companion object : SchemeSpec<SchemeWithInit>(::SchemeWithInit)
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
        val config = MutableMeta()
        val child = config.getOrCreate("child")
        val scheme = TestScheme.write(child)
        scheme.a = 22
        scheme.b = "test"
        assertEquals(22, config["child.a"].int)
        assertEquals("test", config["child.b"].string)
    }

    @Test
    fun testChildUpdate() {
        val config = MutableMeta()
        val child = config.getOrCreate("child")
        child.updateWith(TestScheme) {
            a = 22
            b = "test"
        }
        assertEquals(22, config["child.a"].int)
        assertEquals("test", config["child.b"].string)
    }

    @Test
    fun testSchemeWrappingBeforeEdit() {
        val config = MutableMeta()
        val scheme = TestScheme.write(config)
        scheme.a = 29
        assertEquals(29, config["a"].int)
    }

    @OptIn(DFExperimental::class)
    @Test
    fun testSchemeWrappingAfterEdit() {
        val scheme = TestScheme.empty()
        scheme.a = 29
        val config = MutableMeta()
        scheme.retarget(config)
        assertEquals(29, scheme.a)
    }

    @Test
    fun testSchemeSubscription() {
        val scheme = TestScheme.empty()
        var flag: Int? = null
        scheme.useProperty(TestScheme::a) { a ->
            flag = a
        }
        scheme.a = 2
        assertEquals(2, flag)
    }

    @Test
    fun testListSubscription() {
        val scheme = TestScheme.empty()
        var value: Value? = null
        scheme.v = ListValue(0.0, 0.0, 0.0)
        scheme.useProperty(TestScheme::v) {
            value = it
        }
        scheme.v = ListValue(1.0, 2.0, 3.0)
        assertNotNull(value)
    }

    @Test
    fun testSubScheme() {
        val scheme = TestScheme.empty()

        scheme.sub.subValue = "aaa"

        assertEquals("aaa", scheme.sub.subValue)
    }


    @Test
    fun testSchemeWithInit() {
        val scheme = SchemeWithInit()
        assertEquals("initialValue", scheme.initial)
        scheme.initial = "none"
        assertEquals("none", scheme.initial)
    }

}