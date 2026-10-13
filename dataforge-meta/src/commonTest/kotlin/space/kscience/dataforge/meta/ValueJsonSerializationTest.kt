package space.kscience.dataforge.meta

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import kotlin.test.*

class ValueJsonSerializationTest {
    private val json = Json { encodeDefaults = true }
    private enum class TestEnum { SAMPLE }

    private fun values(): List<Value> = listOf(
        True, False, 0.asValue(), 0.25.asValue(), "".asValue(),
        "17".asValue(), "false".asValue(), "null".asValue(), "native".asValue(),
        Null, ListValue.EMPTY, TestEnum.SAMPLE.asValue(),
        ListValue(listOf(False, Null, ListValue.EMPTY, ListValue(listOf(1.asValue(), "n".asValue())))),
    )

    @Test
    fun jsonUsesNativeValues() {
        values().forEach { value ->
            val encoded = json.encodeToString(ValueSerializer, value)
            assertEquals(value.toJson(), Json.parseToJsonElement(encoded))
        }
    }

    @Test
    fun jsonReadsNativeValues() {
        values().forEach { value ->
            val restored = json.decodeFromString(ValueSerializer, value.toJson().toString())
            assertEquals(value.type, restored.type)
            assertEquals(value.toJson(), restored.toJson())
        }
    }

    @Test
    fun emptyListAndNullStayDifferent() {
        val empty = json.decodeFromString(ValueSerializer, "[]")
        val nullValue = json.decodeFromString(ValueSerializer, "null")
        assertEquals(ValueType.LIST, empty.type)
        assertTrue(empty.list.isEmpty())
        assertSame(Null, nullValue)
        assertNotEquals(empty.toJson(), nullValue.toJson())
    }

    @Test
    fun quotedNumbersAreStrings() {
        for (text in listOf("0", "9007199254740993", "-9223372036854775808")) {
            val value = json.decodeFromString(ValueSerializer, JsonPrimitive(text).toString())
            assertEquals(ValueType.STRING, value.type)
            assertEquals(text, value.string)
        }
    }

    @Test
    fun longIntegersKeepTheirJsonValue() {
        for (number in listOf(Int.MAX_VALUE.toLong() + 1, 9_007_199_254_740_993L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            val restored = json.decodeFromString(ValueSerializer, number.toString())
            assertIs<Long>(assertIs<NumberValue>(restored).number)
            assertEquals(number, restored.number)
            assertEquals(number.toString(), json.encodeToString(ValueSerializer, restored))
        }
        assertIs<Int>(assertIs<NumberValue>(json.decodeFromString(ValueSerializer, "0")).number)
        assertIs<Double>(assertIs<NumberValue>(json.decodeFromString(ValueSerializer, "0.25")).number)
    }

    @Test
    fun jsonObjectsAreNotValues() {
        assertFailsWith<SerializationException> {
            json.decodeFromString(ValueSerializer, """{"value":1}""")
        }
    }

    @Test
    fun descriptorDefaultsRemainNativeValues() {
        for (value in listOf(False, True, 0.asValue(), "native".asValue(), TestEnum.SAMPLE.asValue(), ListValue.EMPTY)) {
            val descriptor = MetaDescriptor(defaultValue = value)
            val encoded = json.encodeToString(MetaDescriptor.serializer(), descriptor)
            Json.parseToJsonElement(encoded)
            val restored = json.decodeFromString(MetaDescriptor.serializer(), encoded)
            assertEquals(value.toJson(), assertNotNull(restored.defaultValue).toJson())
            assertEquals(value.type, restored.defaultValue.type)
        }
    }

    @Test
    fun nullableDescriptorDefaultUsesJsonNull() {
        val absent = json.encodeToString(MetaDescriptor.serializer(), MetaDescriptor())
        val explicitNull = json.encodeToString(MetaDescriptor.serializer(), MetaDescriptor(defaultValue = Null))
        assertEquals(absent, explicitNull)
        assertNull(json.decodeFromString(MetaDescriptor.serializer(), absent).defaultValue)
        assertNull(json.decodeFromString(MetaDescriptor.serializer(), explicitNull).defaultValue)
    }

    @Test
    fun descriptorAllowedValuesRemainNativeMetadata() {
        val descriptor = MetaDescriptor {
            allowedValues(false, true, 0, "native")
        }
        val encoded = json.encodeToString(MetaDescriptor.serializer(), descriptor)
        val restored = json.decodeFromString(MetaDescriptor.serializer(), encoded)
        assertEquals(descriptor.attributes, restored.attributes)
    }
}
