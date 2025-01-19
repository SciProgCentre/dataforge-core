package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals

@DFExperimental
internal class MetaRefTest {

    @Serializable
    data class XY(val x: Double, val y: Double)

    object TestMetaSpec : MetaSpec() {
        val integer by int { description = "Integer value" }
        val string by string { description = "String value" }
        val custom by item(MetaConverter.serializable<XY>()) { description = "custom value" }
    }

    @Test
    fun specWriteRead()  = with(TestMetaSpec){
        val meta = MutableMeta()

        meta[integer] = 22
        meta[string] = "33"
        val xy = XY(33.0, -33.0)
        meta[custom] = xy

        val sealed = meta.seal()

        assertEquals(22, sealed[integer])
        assertEquals("33", sealed[string])
        assertEquals(xy, sealed[custom])
    }
}