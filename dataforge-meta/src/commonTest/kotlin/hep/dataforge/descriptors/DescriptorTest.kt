package hep.dataforge.descriptors

import hep.dataforge.values.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptorTest {

    val descriptor = NodeDescriptor.build {
        node("aNode") {
            info = "A root demo node"
            value("b") {
                info = "b number value"
                type(ValueType.NUMBER)
            }
            node("otherNode") {
                value("otherValue") {
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