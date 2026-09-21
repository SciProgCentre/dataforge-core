package space.kscience.dataforge.meta.descriptors


import io.github.optimumcode.json.schema.ErrorCollector
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import space.kscience.dataforge.meta.DescriptorTest.Choice
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.number
import space.kscience.dataforge.meta.put
import space.kscience.dataforge.meta.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DescriptorJsonSchemaTest {

    private fun objectParent(required: Boolean, nullable: Boolean): MetaDescriptor = MetaDescriptor {
        attributes { "nodeRequired" put true }
        valueTypes = emptyList()
        node("p") {
            attributes { "nodeRequired" put required }
            valueTypes = if (nullable) listOf(ValueType.NULL) else emptyList()
            value("n", ValueType.NUMBER) { valueRestriction = ValueRestriction.REQUIRED }
        }
    }

    @Test
    fun objectPresenceAndNullabilityRoundTripThroughJsonSchema() {
        for (required in listOf(false, true)) {
            for (nullable in listOf(false, true)) {
                val descriptor = objectParent(required, nullable)
                val json = descriptor.toJsonSchema()
                val parent = json["properties"]!!.jsonObject["p"]!!.jsonObject
                assertEquals(
                    if (nullable) JsonArray(listOf(JsonPrimitive("object"), JsonPrimitive("null")))
                    else JsonPrimitive("object"),
                    parent["type"]
                )
                assertEquals(JsonArray(listOf(JsonPrimitive("n"))), parent["required"]!!.jsonArray)
                assertEquals(required, json["required"]?.jsonArray?.contains(JsonPrimitive("p")) == true)
                assertEquals(descriptor, json.toMetaDescriptor())
                val schema = io.github.optimumcode.json.schema.JsonSchema.fromJsonElement(json)
                fun accepts(input: String): Boolean = schema.validate(Json.parseToJsonElement(input), ErrorCollector.EMPTY)
                assertEquals(!required, accepts("{}"))
                assertEquals(nullable, accepts("{\"p\":null}"))
                assertFalse(accepts("{\"p\":{}}"))
                assertFalse(accepts("{\"p\":{\"n\":\"wrong\"}}"))
                assertTrue(accepts("{\"p\":{\"n\":1}}"))
            }
        }
    }

    @Test
    fun plainObjectSchemaKeepsLegacyImport() {
        val descriptor = Json.parseToJsonElement(
            """{"type":"object","properties":{"n":{"type":"number"}},"required":["n"]}"""
        ).jsonObject.toMetaDescriptor()
        assertEquals(Meta.EMPTY, descriptor.attributes)
        assertEquals(null, descriptor.valueTypes)
        assertEquals(ValueRestriction.REQUIRED, descriptor.valueRestriction)
        assertEquals(ValueRestriction.REQUIRED, descriptor.nodes.getValue("n").valueRestriction)
    }

    @Test
    fun unmarkedSchemaKeepsCustomAttributePrecedence() {
        val custom = MetaDescriptor { allowedValues(2) }.toJsonSchema().getValue(JsonSchema.Vocabularies.Custom.ATTRIBUTES)
        val schema = buildJsonObject {
            put("type", "number")
            put("enum", buildJsonArray { add(1) })
            put(JsonSchema.Vocabularies.Custom.ATTRIBUTES, custom)
        }
        assertEquals(listOf(2), schema.toMetaDescriptor().allowedValues!!.map { it.number.toInt() })
    }

    @Test
    fun objectMarkerRejectsConflictingSchemaTypes() {
        val schema = objectParent(false, false).toJsonSchema()
        for (type in listOf(JsonPrimitive("string"), JsonPrimitive("null"), JsonArray(emptyList()))) {
            assertFailsWith<IllegalArgumentException> {
                JsonObject(schema + ("type" to type)).toMetaDescriptor()
            }
        }
        assertFailsWith<IllegalArgumentException> { JsonObject(schema - "type").toMetaDescriptor() }
        assertFailsWith<IllegalArgumentException> {
            JsonObject(schema + ("enum" to JsonArray(listOf(JsonNull)))).toMetaDescriptor()
        }
    }

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
