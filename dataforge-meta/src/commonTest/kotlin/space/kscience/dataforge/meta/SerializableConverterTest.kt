package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.node
import space.kscience.dataforge.names.NameToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class SerializableConverterTest {

    @Serializable
    private data class TestSerializable(
        val a: Int,
        val b: String,
        val c: List<Double> = emptyList(),
    )

    @Serializable
    private data class IndexedItem(val id: String, val value: Double = 0.0)

    @Serializable
    private data class IndexedData(val items: List<IndexedItem>)

    @Test
    fun testSerializableDescriptors() {
        val descriptor = MetaDescriptor { description = "Custom data" }
        assertSame(descriptor, MetaConverter.serializable(TestSerializable.serializer(), descriptor).descriptor)
        assertSame(descriptor, MetaConverter.serializable<TestSerializable>(descriptor).descriptor)

        val derived = MetaDescriptor(TestSerializable.serializer())
        assertEquals(derived, MetaConverter.serializable(TestSerializable.serializer()).descriptor)
        assertEquals(derived, MetaConverter.serializable<TestSerializable>().descriptor)
    }

    @Test
    fun testCustomIndexAndEncoder() {
        val descriptor = MetaDescriptor {
            node("items") {
                multiple = true
                indexKey = "id"
            }
        }
        val json = Json { encodeDefaults = false }
        val converters = listOf(
            MetaConverter.serializable(IndexedData.serializer(), descriptor, json),
            MetaConverter.serializable<IndexedData>(descriptor, json),
        )
        val obj = IndexedData(listOf(IndexedItem("left", 2.0), IndexedItem("right")))
        converters.forEach { converter ->
            val meta = converter.convert(obj)
            assertEquals(setOf(NameToken("items", "left"), NameToken("items", "right")), meta.items.keys)
            assertEquals(2.0, meta["items[left].value"].double)
            assertNull(meta["items[right].value"])
            assertEquals(json.encodeToJsonElement(IndexedData.serializer(), obj), meta.toJson(descriptor))
            assertEquals(obj, converter.read(meta))
        }
    }

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
