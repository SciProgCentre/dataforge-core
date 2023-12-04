package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.ValueType
import space.kscience.dataforge.meta.boolean
import space.kscience.dataforge.meta.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DescriptorTest {

    val descriptor = MetaDescriptor {
        node("aNode") {
            description = "A root demo node"
            value("b", ValueType.NUMBER) {
                description = "b number value"
            }
            node("otherNode") {
                value("otherValue", ValueType.BOOLEAN) {
                    default(false)
                    description = "default value"
                }
            }
        }
    }

    @Test
    fun testAllowedValues() {
        val child = descriptor["aNode.b"]
        assertNotNull(child)
        val allowed = descriptor["aNode"]?.get("b")?.allowedValues
        assertEquals(null, allowed)
    }

    @Test
    fun testDefaultMetaNode() {
        val meta = descriptor.defaultNode
        assertEquals(false, meta["aNode.otherNode.otherValue"].boolean)
    }
}