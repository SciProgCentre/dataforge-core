package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaValidationResult
import space.kscience.dataforge.meta.descriptors.ValueRestriction
import space.kscience.dataforge.meta.descriptors.allowedValues
import space.kscience.dataforge.meta.descriptors.nodeRequired
import space.kscience.dataforge.meta.descriptors.toJsonSchema
import space.kscience.dataforge.meta.descriptors.toMetaDescriptor
import space.kscience.dataforge.meta.descriptors.validate
import space.kscience.dataforge.meta.descriptors.validateWithResult
import space.kscience.dataforge.names.Name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetaDescriptorSerializationTest {

    @Serializable
    data class Child(val n: Int)

    @Serializable
    data class RequiredObject(val p: Child)

    @Serializable
    data class OptionalObject(val p: Child = Child(1))

    @Serializable
    data class RequiredNullableObject(val p: Child?)

    @Serializable
    data class OptionalNullableObject(val p: Child? = null)

    @Serializable
    data class NestedObject(val child: Child)

    @Serializable
    data class ObjectCollections(val list: List<NestedObject>, val map: Map<String, NestedObject>)

    @Serializable
    object EmptyObject

    @Test
    fun optionalNonNullAndRequiredNullableObjectsHaveDifferentDescriptors() {
        assertNotEquals(
            MetaDescriptor(OptionalObject.serializer()).nodes.getValue("p"),
            MetaDescriptor(RequiredNullableObject.serializer()).nodes.getValue("p")
        )
    }

    @Test
    fun objectFieldPresenceSurvivesSerializerDerivation() {
        val descriptors = listOf(
            Triple(MetaDescriptor(RequiredObject.serializer()), true, false),
            Triple(MetaDescriptor(OptionalObject.serializer()), false, false),
            Triple(MetaDescriptor(RequiredNullableObject.serializer()), true, true),
            Triple(MetaDescriptor(OptionalNullableObject.serializer()), false, true)
        )
        for ((descriptor, required, nullable) in descriptors) {
            assertEquals(true, descriptor.nodeRequired)
            val field = descriptor.nodes.getValue("p")
            assertEquals(required, field.nodeRequired)
            assertEquals(ValueRestriction.NONE, field.valueRestriction)
            assertEquals(if (nullable) listOf(ValueType.NULL) else emptyList(), field.valueTypes)
            fun errors(meta: Meta): List<MetaValidationResult.Invalid> =
                descriptor.validateWithResult(meta, Name.EMPTY).filterIsInstance<MetaValidationResult.Invalid>().toList()
            assertEquals(
                if (required) listOf(MetaValidationResult.RequiredValueIsMissing(Name.parse("p"))) else emptyList(),
                errors(Meta.EMPTY)
            )
            assertEquals(nullable, descriptor.validate(Meta { "p" put Null }))
            assertEquals(
                listOf(MetaValidationResult.RequiredValueIsMissing(Name.parse("p.n"))),
                errors(Meta { "p" put Meta.EMPTY })
            )
            assertEquals(
                listOf(MetaValidationResult.IncorrectValueType(Name.parse("p.n"), listOf(ValueType.NUMBER), ValueType.STRING)),
                errors(Meta { "p" put { "n" put "wrong" } })
            )
            assertTrue(descriptor.validate(Meta { "p" put { "n" put 1 } }))
            val restored = Json.decodeFromString<MetaDescriptor>(Json.encodeToString(descriptor))
            assertEquals(descriptor, restored)
            assertEquals(!required, restored.validate(Meta.EMPTY))
            assertEquals(descriptor, descriptor.toJsonSchema().toMetaDescriptor())
        }
    }

    @Test
    fun objectRootsKeepPresenceAndNullability() {
        val empty = MetaDescriptor(EmptyObject.serializer())
        assertEquals(true, empty.nodeRequired)
        assertTrue(empty.validate(Meta.EMPTY))
        assertFalse(empty.validate(null as Meta?))
        assertFalse(empty.validate(Meta(Null)))
        val nullable = MetaDescriptor(Child.serializer().nullable)
        assertFalse(nullable.validate(null as Meta?))
        assertTrue(nullable.validate(Meta(Null)))
        assertFalse(nullable.validate(Meta.EMPTY))
    }

    @Test
    fun listAndMapObjectSubtreesKeepLegacyDescriptors() {
        fun assertLegacy(descriptor: MetaDescriptor) {
            assertNull(descriptor.nodeRequired)
            descriptor.nodes.values.forEach(::assertLegacy)
            descriptor.childrenDescriptor?.let(::assertLegacy)
        }
        val descriptor = MetaDescriptor(ObjectCollections.serializer())
        val list = descriptor.nodes.getValue("list")
        val map = descriptor.nodes.getValue("map")
        assertLegacy(list)
        assertLegacy(map)
        assertTrue(list.multiple)
        assertEquals(ValueRestriction.REQUIRED, list.valueRestriction)
        assertEquals(ValueRestriction.REQUIRED, list.nodes.getValue("child").valueRestriction)
        assertEquals(ValueRestriction.REQUIRED, map.childrenDescriptor!!.nodes.getValue("child").valueRestriction)
    }

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
