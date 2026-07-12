package space.kscience.dataforge.meta.descriptors

import kotlinx.serialization.Serializable
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.*

/**
 * Restrictions on value in the node
 */
@Serializable
public enum class ValueRestriction {
    /**
     * No restrictions
     */
    NONE,

    /**
     * The value is required
     */
    REQUIRED,

    /**
     * The value must be null
     */
    ABSENT
}

/**
 * The descriptor for a meta
 * @param description description text
 * @param nodes child descriptors for this node
 * @param multiple True if same name siblings with this name are allowed
 * @param valueRestriction The requirements for node content
 * @param valueTypes list of allowed types for [Meta.value], null if all values are allowed.
 *  Empty list means that no value should be present in this node.
 * @param indexKey An index field by which this node is identified in case of same name siblings construct
 * @param defaultValue the default [Meta.value] for the node
 * @param childrenDescriptor if present,
 * @param attributes additional attributes of this descriptor. For example, validation and widget parameters
 */
@Serializable
public data class MetaDescriptor(
    public val description: String? = null,
    public val nodes: Map<String, MetaDescriptor> = emptyMap(),
    public val multiple: Boolean = false,
    public val valueRestriction: ValueRestriction = ValueRestriction.NONE,
    public val valueTypes: List<ValueType>? = null,
    public val indexKey: String = Meta.INDEX_KEY,
    public val defaultValue: Value? = null,
    public val childrenDescriptor: MetaDescriptor? = null,
    public val attributes: Meta = Meta.EMPTY
) {
    @Deprecated("Replace by nodes", ReplaceWith("nodes"))
    public val children: Map<String, MetaDescriptor> get() = nodes

    /**
     * A node constructed of default values for this descriptor and its children
     */
    public val defaultNode: Meta by lazy {
        Meta {
            defaultValue?.let { defaultValue ->
                this.value = defaultValue
            }
            nodes.forEach { (key, descriptor) ->
                set(key, descriptor.defaultNode)
            }
        }
    }

    public companion object {
        public val EMPTY: MetaDescriptor = MetaDescriptor("Generic meta tree")
        internal const val ALLOWED_VALUES_KEY = "allowedValues"
    }
}

public val MetaDescriptor.required: Boolean get() = checkRequired(hashSetOf())

private fun MetaDescriptor.checkRequired(visited: MutableSet<MetaDescriptor>): Boolean {
    if (this in visited) return false
    visited.add(this)
    return valueRestriction == ValueRestriction.REQUIRED ||
            nodes.values.any { it.checkRequired(visited) }
}

public val MetaDescriptor.allowedValues: List<Value>? get() = attributes[MetaDescriptor.ALLOWED_VALUES_KEY]?.value?.list

public operator fun MetaDescriptor.get(name: Name): MetaDescriptor? = when (name.length) {
    0 -> this
    1 -> nodes[name.firstOrNull()!!.toString()]
    else -> get(name.firstOrNull()!!.asName())?.get(name.cutFirst())
}

public operator fun MetaDescriptor.get(name: String): MetaDescriptor? = get(name.parseAsName(true))

public sealed interface MetaValidationResult {
    public data object Valid : MetaValidationResult
    public sealed interface Invalid : MetaValidationResult
    public data class RequiredValueIsMissing(val name: Name) : Invalid
    public data class ProhibitedValueIsPresent(val name: Name) : Invalid
    public data class IncorrectValueType(
        val name: Name,
        val expectedType: Collection<ValueType>,
        val actualType: ValueType
    ) : Invalid

    public data class ValueNotAllowed(val name: Name, val expected: Collection<Value>, val actual: Value) : Invalid
}

public val MetaValidationResult.isValid: Boolean get() = this == MetaValidationResult.Valid

/**
 * Check if given [value] adheres to descriptor
 */
public fun MetaDescriptor.validateWithResult(value: Value?, name: Name): MetaValidationResult = when {
    value == null -> if (valueRestriction != ValueRestriction.REQUIRED) {
        MetaValidationResult.Valid
    } else {
        MetaValidationResult.RequiredValueIsMissing(name)
    }

    valueRestriction == ValueRestriction.ABSENT -> MetaValidationResult.ProhibitedValueIsPresent(name)
    valueTypes != null && value.type !in valueTypes -> MetaValidationResult.IncorrectValueType(
        name = name,
        expectedType = valueTypes,
        actualType = value.type
    )

    allowedValues != null -> {
        val allowedValues = allowedValues!!
        if (value.type == ValueType.LIST && multiple) {
            if (value.list.all { it in allowedValues }) {
                MetaValidationResult.Valid
            } else {
                MetaValidationResult.ValueNotAllowed(
                    name = name,
                    expected = allowedValues,
                    actual = value
                )
            }
        } else if (value in allowedValues) {
            MetaValidationResult.Valid
        } else {
            MetaValidationResult.ValueNotAllowed(
                name = name,
                expected = allowedValues,
                actual = value
            )
        }
    }

    else -> MetaValidationResult.Valid
}

/**
 * Validate a meta tree depth-first and return a sequence of all validation results.
 * The validation is failed if at least one of results is [MetaValidationResult.Invalid]
 */
public fun MetaDescriptor.validateWithResult(item: Meta?, name: Name): Sequence<MetaValidationResult> = sequence {
    yield(validateWithResult(item?.value, name))
    nodes.forEach { (key, childDescriptor) ->
        yieldAll(childDescriptor.validateWithResult(item?.get(key), name + key))
    }
}


/**
 * Check if given item suits the descriptor
 */
public fun MetaDescriptor.validate(item: Meta?, name: Name = Name.EMPTY): Boolean =
    validateWithResult(item, name).none { it is MetaValidationResult.Invalid }

