package space.kscience.dataforge.meta.descriptors

import io.github.optimumcode.json.schema.JsonSchema
import space.kscience.dataforge.meta.ValueType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DescriptorJsonSchemaTest {

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

    @BeforeTest
    fun prepareDescriptor() {

    }

    @Test
    fun testIsJsonSchemaValid() {
        // Arrange
        val descriptorJsonSchema = descriptor.toJsonSchema()

        // Act
        val result = runCatching {
            JsonSchema.fromJsonElement(descriptorJsonSchema)
        }

        // Assert
        assertTrue(result.isSuccess, "Expected no exception but got ${result.exceptionOrNull()}")
    }

    @Test
    fun testIsJsonSchemaConvertsToMetaDescriptor() {
        // Arrange
        val descriptorJsonSchema = descriptor.toJsonSchema()

        // Act
        val result = runCatching {
            descriptorJsonSchema.toMetaDescriptor()
        }

        // Assert
        assertTrue(result.isSuccess, "Expected no exception but got ${result.exceptionOrNull()}")
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