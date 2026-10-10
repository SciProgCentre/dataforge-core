package space.kscience.dataforge.meta

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.allowedValues
import space.kscience.dataforge.meta.descriptors.toJsonSchema
import space.kscience.dataforge.meta.descriptors.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
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

    private enum class NumericDisplayChoice {
        FIRST;

        override fun toString(): String = "1"
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
        val multiple = descriptor.copy(multiple = true, valueTypes = listOf(ValueType.STRING, ValueType.LIST))
        assertTrue(multiple.validate(Meta(Value.of(Choice.entries))))
        assertFalse(multiple.validate(Meta(listOf("FIRST", "OTHER").asValue())))
        val numeric = MetaDescriptor {
            this.multiple = true
            valueType(ValueType.NUMBER, ValueType.LIST)
            allowedValues(1)
        }
        assertFalse(numeric.validate(Meta(1.5)))
        assertFalse(numeric.validate(Meta(listOf(1.5).asValue())))
        val numericDisplay = assertNotNull(MetaConverter.enum<NumericDisplayChoice>().descriptor)
            .copy(multiple = true, valueTypes = listOf(ValueType.STRING, ValueType.LIST))
        assertFalse(numericDisplay.validate(Meta(listOf(1).asValue())))
        assertTrue(numericDisplay.validate(Meta(listOf("1").asValue())))
        assertEquals(
            listOf(true, true, true),
            listOf(
                descriptor.validate(Meta("FIRST")), descriptor.validate(Meta("SECOND")),
                multiple.validate(Meta(listOf("FIRST", "SECOND").asValue()))
            )
        )
        assertFalse(descriptor.validate(Meta("OTHER")))
    }

    @Test
    fun enumJsonSchemaUsesScalarNames() {
        val descriptor = assertNotNull(converter.descriptor)
        assertEquals(
            JsonArray(listOf(JsonPrimitive("FIRST"), JsonPrimitive("SECOND"))),
            descriptor.toJsonSchema().getValue("enum"),
        )
        val restored = Json.decodeFromString(
            MetaDescriptor.serializer(), Json.encodeToString(MetaDescriptor.serializer(), descriptor)
        )
        assertTrue(restored.validate(converter.convert(Choice.FIRST)))
    }

    @Test
    fun enumReadsRemainNullableForUnknownNames() {
        val descriptor = assertNotNull(converter.descriptor)
        assertEquals(Choice.FIRST, converter.readOrNull(Meta("FIRST")))
        assertEquals(Choice.SECOND, converter.readOrNull(Meta("SECOND")))
        Choice.entries.forEach { choice ->
            val converted = assertNativeRoundTrip(converter, choice)
            assertEquals(JsonPrimitive(choice.name), converted.toJson())
            val restored = Json.decodeFromString(MetaSerializer, Json.encodeToString(MetaSerializer, converted))
            assertTrue(descriptor.validate(restored))
            assertSame(choice, converter.read(restored))
        }
        assertNull(converter.readOrNull(Meta("OTHER")))
        assertNull(converter.readOrNull(Meta.EMPTY))
    }

    @Test
    fun enumReadsRemainNullableForAnotherEnumType() {
        val foreign = Meta(DisplayChoice.FIRST.asValue())
        assertNull(foreign.enum<Choice>())
        assertNull(converter.readOrNull(foreign))
        assertFailsWith<IllegalStateException> { converter.read(foreign) }
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
