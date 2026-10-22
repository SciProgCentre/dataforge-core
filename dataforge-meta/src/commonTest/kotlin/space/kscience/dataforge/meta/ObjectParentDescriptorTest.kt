package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.*
import space.kscience.dataforge.names.Name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjectParentDescriptorTest {
    private val parent = Name.parse("p")
    private val child = Name.parse("p.n")

    private fun objectDescriptor(required: Boolean, nullable: Boolean = false): MetaDescriptor = MetaDescriptor {
        nodeRequired = required
        valueTypes = if (nullable) listOf(ValueType.NULL) else emptyList()
        value("n", ValueType.NUMBER) { valueRestriction = ValueRestriction.REQUIRED }
    }

    private fun MetaDescriptor.errors(item: Meta?): List<MetaValidationResult.Invalid> =
        validateWithResult(item, parent).filterIsInstance<MetaValidationResult.Invalid>().toList()

    @Test
    fun absentOptionalParentDoesNotRequireDescendants() {
        for (nullable in listOf(false, true)) {
            val descriptor = objectDescriptor(required = false, nullable = nullable)
            assertEquals(emptyList(), descriptor.errors(null))
            assertFalse(descriptor.required)
        }
    }

    @Test
    fun presentObjectParentStillRequiresChildren() {
        for (required in listOf(false, true)) {
            val descriptor = objectDescriptor(required)
            assertEquals(listOf(MetaValidationResult.RequiredValueIsMissing(child)), descriptor.errors(Meta.EMPTY))
            assertEquals(
                listOf(MetaValidationResult.IncorrectValueType(child, listOf(ValueType.NUMBER), ValueType.STRING)),
                descriptor.errors(Meta { "n" put "wrong" })
            )
            assertEquals(emptyList(), descriptor.errors(Meta { "n" put 1; "extra" put true }))
        }
    }

    @Test
    fun nullLeafFollowsExistingValueTypes() {
        for (required in listOf(false, true)) {
            assertEquals(emptyList(), objectDescriptor(required = required, nullable = true).errors(Meta(Null)))
            assertEquals(
                listOf(MetaValidationResult.IncorrectValueType(parent, emptyList(), ValueType.NULL)),
                objectDescriptor(required).errors(Meta(Null))
            )
            for (value in listOf("scalar".asValue(), listOf(1, 2).asValue())) {
                assertEquals(
                    listOf(MetaValidationResult.IncorrectValueType(parent, emptyList(), value.type)),
                    objectDescriptor(required).errors(Meta(value))
                )
            }
        }
    }

    @Test
    fun requiredParentReportsItsOwnPath() {
        for (nullable in listOf(false, true)) {
            val descriptor = objectDescriptor(required = true, nullable = nullable)
            assertTrue(descriptor.required)
            assertEquals(listOf(MetaValidationResult.RequiredValueIsMissing(parent)), descriptor.errors(null))
            val empty = descriptor.copy(nodes = emptyMap())
            assertEquals(listOf(MetaValidationResult.RequiredValueIsMissing(parent)), empty.errors(null))
            assertEquals(emptyList(), empty.errors(Meta.EMPTY))
        }
    }

    @Test
    fun legacyRequiredDescendantStillRequiresItsPath() {
        val descriptor = objectDescriptor(false).copy(attributes = Meta.EMPTY)
        assertTrue(descriptor.required)
        assertEquals(listOf(MetaValidationResult.RequiredValueIsMissing(child)), descriptor.errors(null))
    }

    @Test
    fun hybridMetaIsRejectedOnlyInObjectMode() {
        val hybrid = Meta { value = Null; "n" put 1 }
        assertEquals(
            listOf(MetaValidationResult.ProhibitedValueIsPresent(parent)),
            objectDescriptor(required = false, nullable = true).errors(hybrid)
        )
        assertEquals(
            listOf(MetaValidationResult.IncorrectValueType(parent, emptyList(), ValueType.NULL)),
            objectDescriptor(false).errors(hybrid)
        )
        assertEquals(emptyList(), objectDescriptor(required = false, nullable = true).copy(attributes = Meta.EMPTY).errors(hybrid))
    }

    @Test
    fun conflictingObjectDescriptorIsRejected() {
        val descriptor = objectDescriptor(false)
        val conflicts = listOf(
            "required value" to { descriptor.copy(valueRestriction = ValueRestriction.REQUIRED) },
            "absent value" to { descriptor.copy(valueRestriction = ValueRestriction.ABSENT) },
            "unrestricted value types" to { descriptor.copy(valueTypes = null) },
            "string value type" to { descriptor.copy(valueTypes = listOf(ValueType.STRING)) },
            "nullable number value type" to { descriptor.copy(valueTypes = listOf(ValueType.NULL, ValueType.NUMBER)) },
            "allowed values" to { descriptor.copy { allowedValues(1) } },
            "string nodeRequired marker" to { descriptor.copy(attributes = Meta { "nodeRequired" put "false" }) },
            "number nodeRequired marker" to { descriptor.copy(attributes = Meta { "nodeRequired" put 0 }) },
            "null nodeRequired marker" to { descriptor.copy(attributes = Meta { "nodeRequired" put Null }) }
        )
        for ((label, conflict) in conflicts) {
            assertFailsWith<IllegalArgumentException>(label) { conflict() }
        }
    }

    @Test
    fun objectDescriptorIsCheckedAtConstruction() {
        assertFailsWith<IllegalArgumentException> {
            MetaDescriptor(attributes = Meta { "nodeRequired" put false })
        }
        assertFailsWith<IllegalArgumentException> {
            MetaDescriptor {
                nodeRequired = false
                valueType(ValueType.NUMBER)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            objectDescriptor(false).copy { required() }
        }
    }

    @Test
    fun clearingNodeRequiredRestoresLegacyValidation() {
        val descriptor = MetaDescriptor {
            nodeRequired = false
            assertEquals(false, nodeRequired)
            valueTypes = emptyList()
            value("n", ValueType.NUMBER) { required() }
        }
        assertEquals(false, descriptor.nodeRequired)
        val legacy = descriptor.copy { nodeRequired = null }
        assertEquals(null, legacy.nodeRequired)
        assertTrue(legacy.required)
        assertEquals(listOf(MetaValidationResult.RequiredValueIsMissing(child)), legacy.errors(null))
    }
}
