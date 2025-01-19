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
    public val attributes: Meta = Meta.EMPTY,
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

public val MetaDescriptor.required: Boolean get() = valueRestriction == ValueRestriction.REQUIRED || nodes.values.any { required }

public val MetaDescriptor.allowedValues: List<Value>? get() = attributes[MetaDescriptor.ALLOWED_VALUES_KEY]?.value?.list

public operator fun MetaDescriptor.get(name: Name): MetaDescriptor? = when (name.length) {
    0 -> this
    1 -> nodes[name.firstOrNull()!!.toString()]
    else -> get(name.firstOrNull()!!.asName())?.get(name.cutFirst())
}

public operator fun MetaDescriptor.get(name: String): MetaDescriptor? = get(name.parseAsName(true))

public fun MetaDescriptor.validate(value: Value?): Boolean = if (value == null) {
    valueRestriction != ValueRestriction.REQUIRED
} else {
    if (valueRestriction == ValueRestriction.ABSENT) false
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

    nodes.forEach { (key, childDescriptor) ->
        if (!childDescriptor.validate(item[key])) return false
    }
    return true
}

