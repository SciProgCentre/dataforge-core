package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.*
import kotlin.test.*

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

    @Test
    fun testValidationWithAllowedValues() {
        val descriptor = MetaDescriptor {
            value("a", ValueType.STRING) {
                allowedValues("v1", "v2")
            }
        }
        assertTrue(descriptor.validate(Meta { "a" put "v1" }))
        assertTrue(descriptor.validate(Meta { "a" put "v2" }))
        assertFalse(descriptor.validate(Meta { "a" put "v3" }))
    }

    @Test
    fun testValidationWithListAndMultiple() {
        val descriptor = MetaDescriptor {
            value("a", ValueType.LIST) {
                allowedValues("v1", "v2")
                multiple = true
            }
        }
        assertTrue(descriptor.validate(Meta { "a" put Value.of(listOf("v1", "v2")) }))
        assertTrue(descriptor.validate(Meta { "a" put Value.of(listOf("v1")) }))
        assertFalse(descriptor.validate(Meta { "a" put Value.of(listOf("v1", "v3")) }))
    }

    @Test
    fun testValidationWithListAndNotMultiple() {
        val descriptor = MetaDescriptor {
            value("a", ValueType.LIST) {
                allowedValues(listOf("v1", "v2"))
                multiple = false
            }
        }
        // In this case the whole list must be in allowedValues
        assertTrue(descriptor.validate(Meta { "a" put Value.of(listOf("v1", "v2")) }))
        assertFalse(descriptor.validate(Meta { "a" put "v1" }))
    }

    @Test
    fun testValidationWithValueRestriction() {
        val requiredDescriptor = MetaDescriptor {
            value("required", ValueType.STRING) {
                valueRestriction = ValueRestriction.REQUIRED
            }
        }

        assertTrue(requiredDescriptor.validate(Meta { "required" put "present" }))
        assertFalse(requiredDescriptor.validate(Meta { "required" put null }))

        val prohibitedDescriptor = MetaDescriptor {
            value("prohibited", ValueType.STRING) {
                valueRestriction = ValueRestriction.ABSENT
            }
        }
        assertFalse(prohibitedDescriptor.validate(Meta { "prohibited" put "present" }))
        assertTrue(prohibitedDescriptor.validate(Meta { "prohibited" put null }))
    }

    @Test
    fun testValidationWithValueTypes() {
        val descriptor = MetaDescriptor {
            value("number", ValueType.NUMBER)
        }

        assertTrue(descriptor.validate(Meta { "number" put 1 }))
        assertTrue(descriptor.validate(Meta { "number" put 1.1 }))
        assertFalse(descriptor.validate(Meta { "number" put "string" }))
    }
}