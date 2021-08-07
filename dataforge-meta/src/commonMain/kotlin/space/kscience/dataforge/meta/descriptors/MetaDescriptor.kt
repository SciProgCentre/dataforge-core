package space.kscience.dataforge.meta.descriptors

import kotlinx.serialization.Serializable
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType

/**
 * Restrictions on value in the node
 */
public enum class ValueRequirement {
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
 * @param info description text
 * @param children child descriptors for this node
 * @param multiple True if same name siblings with this name are allowed
 * @param required The requirements for node content
 * @param valueTypes list of allowed types for [Meta.value], null if all values are allowed.
 *  Empty list means that no value should be present in this node.
 * @param indexKey An index field by which this node is identified in case of same name siblings construct
 * @param defaultValue the default [Meta.value] for the node
 * @param attributes additional attributes of this descriptor. For example validation and widget parameters
 */
@Serializable
public data class MetaDescriptor(
    public val info: String? = null,
    public val children: Map<String, MetaDescriptor> = emptyMap(),
    public val multiple: Boolean = false,
    public val valueRequirement: ValueRequirement = ValueRequirement.NONE,
    public val valueTypes: List<ValueType>? = null,
    public val indexKey: String = Meta.INDEX_KEY,
    public val defaultValue: Value? = null,
    public val attributes: Meta = Meta.EMPTY,
) {
    public companion object {
        internal const val ALLOWED_VALUES_KEY = "allowedValues"
    }
}

public val MetaDescriptor.required: Boolean get() = valueRequirement == ValueRequirement.REQUIRED || children.values.any { required }

public val MetaDescriptor.allowedValues: List<Value>? get() = attributes[MetaDescriptor.ALLOWED_VALUES_KEY]?.value?.list

public operator fun MetaDescriptor.get(name: Name): MetaDescriptor? = when (name.length) {
    0 -> this
    1 -> children[name.firstOrNull()!!.toString()]
    else -> get(name.firstOrNull()!!.asName())?.get(name.cutFirst())
}

public operator fun MetaDescriptor.get(name: String): MetaDescriptor? = get(Name.parse(name))

/**
 * A node constructed of default values for this descriptor and its children
 */
public val MetaDescriptor.defaultNode: Meta
    get() = Meta {
        defaultValue?.let { defaultValue ->
            this.value = defaultValue
        }
        children.forEach { (key, descriptor) ->
            set(key, descriptor.defaultNode)
        }
    }

public fun MetaDescriptor.validate(value: Value?): Boolean = if (value == null) {
    valueRequirement != ValueRequirement.REQUIRED
} else {
    if (valueRequirement == ValueRequirement.ABSENT) false
    else {
        (valueTypes == null || value.type in valueTypes) && (allowedValues?.let { value in it } ?: true)
    }
}

/**
 * Check if given item suits the descriptor
 */
public fun MetaDescriptor.validate(item: Meta?): Boolean {
    if (item == null) return !required
    if (!validate(item.value)) return false

    children.forEach { (key, childDescriptor) ->
        if (!childDescriptor.validate(item[key])) return false
    }
    return true
}

