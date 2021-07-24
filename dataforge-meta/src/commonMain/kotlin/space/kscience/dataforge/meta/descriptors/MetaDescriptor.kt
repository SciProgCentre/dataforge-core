package space.kscience.dataforge.meta.descriptors

import kotlinx.serialization.Serializable
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType

/**
 * The descriptor for a meta
 * @param info description text
 * @param children child descriptors for this node
 * @param multiple True if same name siblings with this name are allowed
 * @param required True if the item is required
 * @param type list of allowed types for [Meta.value], null if all values are allowed
 * @param indexKey An index field by which this node is identified in case of same name siblings construct
 * @param defaultValue the default [Meta.value] for the node
 * @param attributes additional attributes of this descriptor. For example validation and widget parameters
 */
@Serializable
public data class MetaDescriptor(
    public val info: String? = null,
    public val children: Map<String, MetaDescriptor> = emptyMap(),
    public val multiple: Boolean = false,
    public val required: Boolean = false,
    public val type: List<ValueType>? = null,
    public val indexKey: String = Meta.INDEX_KEY,
    public val defaultValue: Value? = null,
    public val attributes: Meta = Meta.EMPTY,
)

public operator fun MetaDescriptor.get(name: Name): MetaDescriptor? = when (name.length) {
    0 -> this
    1 -> children[name.firstOrNull()!!.toString()]
    else -> get(name.firstOrNull()!!.asName())?.get(name.cutFirst())
}

public operator fun MetaDescriptor.get(name: String): MetaDescriptor? = get(name.toName())

public class MetaDescriptorBuilder {
    public var info: String? = null
    public var children: MutableMap<String, MetaDescriptor> = hashMapOf()
    public var multiple: Boolean = false
    public var required: Boolean = false
    public var type: List<ValueType>? = null
    public var indexKey: String = Meta.INDEX_KEY
    public var default: Value? = null
    public var attributes: Meta = Meta.EMPTY

    internal fun build(): MetaDescriptor = MetaDescriptor(
        info = info,
        children = children,
        multiple = multiple,
        required = required,
        type = type,
        indexKey = indexKey,
        defaultValue = default,
        attributes = attributes
    )
}