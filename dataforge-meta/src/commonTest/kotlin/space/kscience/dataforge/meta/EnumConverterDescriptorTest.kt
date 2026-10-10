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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EnumConverterDescriptorTest {
    private enum class Choice { FIRST, SECOND }

    private enum class DisplayChoice {
        FIRST, SECOND;

        override fun toString(): String = "display:$name"
    }

    private val converter = MetaConverter.enum<Choice>()

    @Test
    fun enumDescriptorKeepsEachNativeScalarValue() {
        val descriptor = assertNotNull(converter.descriptor)
        val allowed = assertNotNull(descriptor.allowedValues)
        assertEquals(Choice.entries.size, allowed.size)
        Choice.entries.forEachIndexed { index, choice ->
            val value = assertIs<EnumValue<*>>(allowed[index])
            assertEquals(ValueType.STRING, value.type)
            assertSame(choice, value.value)
            assertNativeRoundTrip(converter, choice)
        }
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
        Choice.entries.forEach { choice ->
            val converted = assertNativeRoundTrip(converter, choice)
            assertEquals(JsonPrimitive(choice.name), converted.toJson())
            val restored = Json.decodeFromString(MetaSerializer, Json.encodeToString(MetaSerializer, converted))
            assertSame(choice, converter.read(restored))
        }
        assertNull(converter.readOrNull(Meta("OTHER")))
        assertNull(converter.readOrNull(Meta.EMPTY))
    }

    @Test
    fun enumsWithCustomDisplayKeepNativeValuesAndNameReading() {
        val displayConverter = MetaConverter.enum<DisplayChoice>()
        val descriptor = assertNotNull(displayConverter.descriptor)
        val allowed = assertNotNull(descriptor.allowedValues)
        assertEquals(DisplayChoice.entries.size, allowed.size)
        DisplayChoice.entries.forEachIndexed { index, choice ->
            val value = assertIs<EnumValue<*>>(allowed[index])
            assertEquals(ValueType.STRING, value.type)
            assertSame(choice, value.value)
            assertNativeRoundTrip(displayConverter, choice)
            assertSame(choice, displayConverter.readOrNull(Meta(choice.name)))
            assertNull(displayConverter.readOrNull(Meta(choice.toString())))
        }
        assertNull(displayConverter.readOrNull(Meta("OTHER")))
        assertNull(displayConverter.readOrNull(Meta.EMPTY))
    }

    private fun <E : Enum<E>> assertNativeRoundTrip(converter: MetaConverter<E>, value: E): Meta {
        val descriptor = assertNotNull(converter.descriptor)
        val converted = converter.convert(value)
        assertSame(value, assertIs<EnumValue<*>>(converted.value).value)
        assertTrue(descriptor.validate(converted))
        assertSame(value, converter.read(converted))
        return converted
    }
}
