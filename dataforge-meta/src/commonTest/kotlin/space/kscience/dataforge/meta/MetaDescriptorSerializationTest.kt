package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.ValueRestriction
import space.kscience.dataforge.meta.descriptors.allowedValues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetaDescriptorSerializationTest {

    @Serializable
    data class TestData(
        val a: Int,
        val b: String?,
        val c: List<Double>,
        val d: List<TestDataChild>,
        val e: TestEnum
    )

    @Serializable
    data class TestDataChild(
        val f: Boolean
    )

    enum class TestEnum {
        V1, V2
    }

    @Test
    fun testFromSerialDescriptor() {
        val descriptor = TestData.serializer().descriptor
        val metaDescriptor = MetaDescriptor(descriptor)

        assertNotNull(metaDescriptor.nodes["a"])
        assertEquals(listOf(ValueType.NUMBER), metaDescriptor.nodes["a"]?.valueTypes)
        assertEquals(ValueRestriction.REQUIRED, metaDescriptor.nodes["a"]?.valueRestriction)

        assertNotNull(metaDescriptor.nodes["b"])
        assertEquals(listOf(ValueType.STRING, ValueType.NULL), metaDescriptor.nodes["b"]?.valueTypes)
        assertEquals(ValueRestriction.NONE, metaDescriptor.nodes["b"]?.valueRestriction)

        assertNotNull(metaDescriptor.nodes["c"])
        assertEquals(listOf(ValueType.LIST), metaDescriptor.nodes["c"]?.valueTypes)

        assertNotNull(metaDescriptor.nodes["d"])
        assertTrue(metaDescriptor.nodes["d"]?.multiple ?: false)
        assertNotNull(metaDescriptor.nodes["d"]?.nodes?.get("f"))

        assertNotNull(metaDescriptor.nodes["e"])
        assertEquals(listOf(ValueType.STRING), metaDescriptor.nodes["e"]?.valueTypes)
        assertEquals(listOf("V1", "V2"), metaDescriptor.nodes["e"]?.allowedValues?.map { it.string })
    }
}
