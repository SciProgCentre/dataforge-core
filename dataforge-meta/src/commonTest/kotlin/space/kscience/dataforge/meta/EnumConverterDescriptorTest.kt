package space.kscience.dataforge.meta

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import space.kscience.dataforge.meta.descriptors.allowedValues
import space.kscience.dataforge.meta.descriptors.toJsonSchema
import space.kscience.dataforge.meta.descriptors.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnumConverterDescriptorTest {
    private enum class Choice { FIRST, SECOND }

    private enum class DisplayChoice {
        FIRST, SECOND;

        override fun toString(): String = "display:$name"
    }

    private val converter = MetaConverter.enum<Choice>()

    @Test
    fun enumDescriptorAcceptsEachScalarName() {
        val descriptor = assertNotNull(converter.descriptor)
        val allowed = assertNotNull(descriptor.allowedValues)
        assertEquals(listOf("FIRST", "SECOND"), allowed.map { it.string })
        assertEquals(listOf(ValueType.STRING, ValueType.STRING), allowed.map { it.type })
        assertTrue(descriptor.validate(Meta("FIRST")))
        assertTrue(descriptor.validate(Meta("SECOND")))
        assertTrue(descriptor.validate(converter.convert(Choice.FIRST)))
        assertTrue(descriptor.validate(converter.convert(Choice.SECOND)))
        assertFalse(descriptor.validate(Meta("OTHER")))
    }

    @Test
    fun enumJsonSchemaUsesScalarNames() {
        val descriptor = assertNotNull(converter.descriptor)
        assertEquals(
            JsonArray(listOf(JsonPrimitive("FIRST"), JsonPrimitive("SECOND"))),
            descriptor.toJsonSchema().getValue("enum"),
        )
    }

    @Test
    fun enumReadsRemainNullableForUnknownNames() {
        assertEquals(Choice.FIRST, converter.readOrNull(Meta("FIRST")))
        assertEquals(Choice.SECOND, converter.readOrNull(Meta("SECOND")))
        Choice.entries.forEach { assertRoundTrip(converter, it) }
        assertNull(converter.readOrNull(Meta("OTHER")))
        assertNull(converter.readOrNull(Meta.EMPTY))
    }

    @Test
    fun enumsWithCustomDisplayUseCanonicalNames() {
        val displayConverter = MetaConverter.enum<DisplayChoice>()
        val descriptor = assertNotNull(displayConverter.descriptor)
        assertEquals(listOf("FIRST", "SECOND"), descriptor.allowedValues?.map { it.string })
        assertEquals(
            JsonArray(listOf(JsonPrimitive("FIRST"), JsonPrimitive("SECOND"))),
            descriptor.toJsonSchema().getValue("enum"),
        )
        DisplayChoice.entries.forEach { assertRoundTrip(displayConverter, it) }
        assertNull(displayConverter.readOrNull(Meta("display:FIRST")))
        assertNull(displayConverter.readOrNull(Meta("OTHER")))
        assertNull(displayConverter.readOrNull(Meta.EMPTY))
    }

    private fun <E : Enum<E>> assertRoundTrip(converter: MetaConverter<E>, value: E) {
        val descriptor = assertNotNull(converter.descriptor)
        val converted = converter.convert(value)
        assertTrue(descriptor.validate(converted))
        assertEquals(value, converter.read(converted))
        assertEquals(JsonPrimitive(value.name), converted.toJson())
        val restored = Json.decodeFromString(MetaSerializer, Json.encodeToString(MetaSerializer, converted))
        assertTrue(descriptor.validate(restored))
        assertEquals(value, converter.read(restored))
    }
}
