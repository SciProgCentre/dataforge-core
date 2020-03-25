package hep.dataforge.meta.descriptors

import hep.dataforge.values.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptorTest {

    val descriptor = NodeDescriptor {
        defineNode("aNode") {
            info = "A root demo node"
            defineValue("b") {
                info = "b number value"
                type(ValueType.NUMBER)
            }
            defineNode("otherNode") {
                defineValue("otherValue") {
                    type(ValueType.BOOLEAN)
                    default(false)
                    info = "default value"
                }
            }
        }
    }

    @Test
    fun testAllowedValues() {
        val allowed = descriptor.nodes["aNode"]?.values?.get("b")?.allowedValues
        assertEquals(allowed, emptyList())
    }
}