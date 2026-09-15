package space.kscience.dataforge.meta.descriptors


import io.github.optimumcode.json.schema.ErrorCollector
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import space.kscience.dataforge.meta.DescriptorTest.Choice
import space.kscience.dataforge.meta.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DescriptorJsonSchemaTest {

    @Test
    fun testNullableEnumJsonSchema() {
        val descriptor = MetaDescriptor(serializer<Choice?>())
        val schema = io.github.optimumcode.json.schema.JsonSchema.fromJsonElement(descriptor.toJsonSchema())
        assertTrue(schema.validate(JsonPrimitive("A"), ErrorCollector.EMPTY))
        assertTrue(schema.validate(JsonPrimitive("B"), ErrorCollector.EMPTY))
        assertTrue(schema.validate(JsonNull, ErrorCollector.EMPTY))
        assertFalse(schema.validate(JsonPrimitive("C"), ErrorCollector.EMPTY))
        assertFalse(schema.validate(JsonPrimitive(true), ErrorCollector.EMPTY))

        val nonNullable = MetaDescriptor(serializer<Choice>())
        val nonNullableSchema = io.github.optimumcode.json.schema.JsonSchema.fromJsonElement(nonNullable.toJsonSchema())
        assertTrue(nonNullableSchema.validate(JsonPrimitive("A"), ErrorCollector.EMPTY))
        assertFalse(nonNullableSchema.validate(JsonNull, ErrorCollector.EMPTY))
    }

    val descriptor = MetaDescriptor {
        node("aNode") {
            description = "A root demo node"
            value("b", ValueType.NUMBER) {
                description = "b number value"
                required()
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
    fun testIsJsonSchemaValid() {
        // Arrange
        val descriptorJsonSchema = descriptor.toJsonSchema()

        // Act & Assert
        io.github.optimumcode.json.schema.JsonSchema.fromJsonElement(descriptorJsonSchema)
    }

    @Test
    fun testIsJsonSchemaConvertsToMetaDescriptor() {
        // Arrange
        val descriptorJsonSchema = descriptor.toJsonSchema()

        // Act & Assert
        descriptorJsonSchema.toMetaDescriptor()
    }

    @Test
    fun testIsSerializationAndDeserializationWorksCorrect() {
        // Arrange
        val descriptorWithExplicitRequired = descriptor.applyRequiredRestrictions()
        val descriptorJsonSchema = descriptorWithExplicitRequired.toJsonSchema()

        // Act
        val descriptorFromJsonSchema = descriptorJsonSchema.toMetaDescriptor()

        // Assert
        assertEquals(descriptorWithExplicitRequired, descriptorFromJsonSchema, "Expected equal descriptors")
    }

    fun MetaDescriptor.applyRequiredRestrictions(): MetaDescriptor =
        this.copy {
            if (this@applyRequiredRestrictions.required) {
                valueRestriction = ValueRestriction.REQUIRED
            }
            nodes.forEach { (name, childDescriptor) ->
                node(name, childDescriptor.applyRequiredRestrictions())
            }
        }
}
