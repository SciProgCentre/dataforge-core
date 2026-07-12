package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializableConverterTest {

    @Serializable
    private data class TestSerializable(
        val a: Int,
        val b: String,
        val c: List<Double> = emptyList(),
    )

    @Test
    fun testSerializableConversion() {
        val converter = MetaConverter.serializable<TestSerializable>()
        val obj = TestSerializable(a = 1, b = "test", c = listOf(1.0, 2.0))
        val meta = converter.convert(obj)
        val reconstructed = converter.read(meta)
        assertEquals(obj, reconstructed)
    }

    @Test
    fun testNestedSerializable() {
        @Serializable
        data class Nested(val inner: TestSerializable)

        val converter = MetaConverter.serializable<Nested>()
        val obj = Nested(TestSerializable(a = 1, b = "test"))
        val meta = converter.convert(obj)
        val reconstructed = converter.read(meta)
        assertEquals(obj, reconstructed)
    }

    @Test
    fun testCustomJson() {
        @Serializable
        data class WithDefault(val a: Int, val b: String = "default")

        val json = Json { encodeDefaults = false }
        val converter = MetaConverter.serializable<WithDefault>(jsonEncoder = json)
        
        val obj = WithDefault(a = 1)
        val meta = converter.convert(obj)
        
        // b should not be in meta because encodeDefaults = false
        assertEquals(null, meta["b"])
        
        val reconstructed = converter.read(meta)
        assertEquals(obj, reconstructed)
    }
}
