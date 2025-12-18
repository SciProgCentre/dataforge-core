package space.kscience.dataforge.meta.descriptors


import space.kscience.dataforge.meta.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals

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