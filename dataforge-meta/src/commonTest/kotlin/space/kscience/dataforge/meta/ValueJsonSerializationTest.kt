package space.kscience.dataforge.meta

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import kotlin.test.*

class ValueJsonSerializationTest {
    private val json = Json { encodeDefaults = true }
    private enum class TestEnum { SAMPLE }

    private fun nativeValues(): List<Pair<Value, String>> = listOf(
        True to "true",
        False to "false",
        0.asValue() to "0",
        0.25.asValue() to "0.25",
        "".asValue() to "\"\"",
        "17".asValue() to "\"17\"",
        "false".asValue() to "\"false\"",
        "null".asValue() to "\"null\"",
        "native".asValue() to "\"native\"",
        Null to "null",
        ListValue.EMPTY to "[]",
        TestEnum.SAMPLE.asValue() to "\"SAMPLE\"",
        ListValue(listOf(False, Null, ListValue.EMPTY, ListValue(listOf(1.asValue(), "n".asValue())))) to "[false,null,[],[1,\"n\"]]",
    )

    @Test
    fun jsonUsesNativeValues() {
        for ((value, expected) in nativeValues()) {
            val encoded = json.encodeToString(ValueSerializer, value)
            assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(encoded), expected)
        }
    }

    @Test
    fun jsonReadsNativeValues() {
        for ((value, encoded) in nativeValues()) {
            val restored = json.decodeFromString(ValueSerializer, encoded)
            assertEquals(value.type, restored.type, encoded)
            assertEquals(Json.parseToJsonElement(encoded), restored.toJson(), encoded)
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
    fun descriptorFalseDefaultUsesJsonBoolean() {
        val descriptor = MetaDescriptor { default(false) }
        val encoded = json.encodeToString(MetaDescriptor.serializer(), descriptor)
        assertEquals(JsonPrimitive(false), Json.parseToJsonElement(encoded).jsonObject["defaultValue"])
        assertEquals(descriptor, json.decodeFromString(MetaDescriptor.serializer(), encoded))
    }

    @Test
    fun descriptorDefaultsRemainNativeValues() {
        for (value in listOf(False, True, 0.asValue(), "native".asValue(), TestEnum.SAMPLE.asValue(), ListValue.EMPTY)) {
            val descriptor = MetaDescriptor(defaultValue = value)
            val encoded = json.encodeToString(MetaDescriptor.serializer(), descriptor)
            assertEquals(value.toJson(), Json.parseToJsonElement(encoded).jsonObject["defaultValue"])
            val restored = json.decodeFromString(MetaDescriptor.serializer(), encoded)
            assertEquals(value.toJson(), assertNotNull(restored.defaultValue).toJson())
            assertEquals(value.type, restored.defaultValue.type)
        }
    }

    @Test
    fun nullDescriptorDefaultIsNormalizedToAbsent() {
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
