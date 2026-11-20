package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import space.kscience.dataforge.meta.descriptors.*
import kotlin.test.*

class DescriptorTest {

    @Serializable
    enum class Choice { A, B }

    @Serializable
    data class NullableValues(val number: Double?, val text: String?, val flag: Boolean?, val choice: Choice?)

    @Test
    fun testNullableScalarDescriptors() {
        val cases = listOf(
            MetaDescriptor(serializer<Double?>()) to 1.5.asValue(),
            MetaDescriptor(serializer<String?>()) to "text".asValue(),
            MetaDescriptor(serializer<Boolean?>()) to true.asValue(),
        )
        cases.forEach { (descriptor, value) ->
            assertEquals(listOf(value.type, ValueType.NULL), descriptor.valueTypes)
            assertTrue(descriptor.validate(Meta(value)))
            assertTrue(descriptor.validate(Meta(Null)))
            assertFalse(descriptor.validate(Meta(ListValue.EMPTY)))
        }
    }

    @Test
    fun testNonNullableScalarDescriptors() {
        val cases = listOf(
            MetaDescriptor(serializer<Double>()) to 1.5.asValue(),
            MetaDescriptor(serializer<String>()) to "text".asValue(),
            MetaDescriptor(serializer<Boolean>()) to true.asValue(),
            MetaDescriptor(serializer<Choice>()) to "A".asValue(),
        )
        cases.forEach { (descriptor, value) ->
            assertEquals(listOf(value.type), descriptor.valueTypes)
            assertTrue(descriptor.validate(Meta(value)))
            assertFalse(descriptor.validate(Meta(Null)))
        }
    }

    @Test
    fun testNullableEnumDescriptor() {
        val descriptor = MetaDescriptor(serializer<Choice?>())
        assertEquals(listOf(ValueType.STRING, ValueType.NULL), descriptor.valueTypes)
        assertEquals(listOf("A".asValue(), "B".asValue(), Null), descriptor.allowedValues)
        assertTrue(descriptor.validate(Meta("A")))
        assertTrue(descriptor.validate(Meta(Null)))
        assertFalse(descriptor.validate(Meta("C")))
        assertFalse(descriptor.validate(Meta(true)))
    }

    @Test
    fun testNullableFieldDescriptors() {
        val descriptor = MetaDescriptor(NullableValues.serializer())
        val values = mapOf("number" to 1.5, "text" to "text", "flag" to true, "choice" to "A")
        values.forEach { (name, value) ->
            val child = assertNotNull(descriptor[name])
            assertTrue(child.validate(Meta(Value.of(value))), name)
            assertTrue(child.validate(Meta(Null)), name)
            assertFalse(child.validate(Meta(ListValue.EMPTY)), name)
            assertEquals(ValueRestriction.NONE, child.valueRestriction)
            assertTrue(child.validate(null), name)
            assertTrue(child.validate(Meta.EMPTY), name)
        }
        assertTrue(descriptor.validate(Meta.EMPTY))
        assertFailsWith<SerializationException> { Json.decodeFromString<NullableValues>("{}") }
    }

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

    @Test
    fun testMultipleElementsInList() {
        val descriptor = MetaDescriptor {
            node("list") {
                valueType(ValueType.NUMBER, ValueType.LIST, ValueType.NULL)
                multiple = true
                default(listOf(0))
                allowedValues(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            }
        }


        assertTrue(descriptor.validate(Meta {
            "list" put ListValue(1, 3, 8)
        }))

        assertFalse(descriptor.validate(Meta {
            "list" put ListValue(1, 3, 11)
        }))

        assertTrue(descriptor.validate(Meta { "list" put 2 }))
        assertFalse(descriptor.validate(Meta { "list" put -2 }))

        assertTrue(descriptor.validate(Meta { "list" put listOf<Value>().asValue() }))

    }
}
