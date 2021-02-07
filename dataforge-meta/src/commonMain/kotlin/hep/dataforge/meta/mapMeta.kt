package hep.dataforge.meta

import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.misc.DFExperimental
import hep.dataforge.names.toName
import hep.dataforge.values.ListValue
import hep.dataforge.values.Value

/**
 * Convert meta to map of maps
 */
public fun Meta.toMap(descriptor: NodeDescriptor? = null): Map<String, Any?> {
    return items.entries.associate { (token, item) ->
        token.toString() to when (item) {
            is MetaItemNode -> item.node.toMap()
            is MetaItemValue -> item.value.value
        }
    }
}

/**
 * Convert map of maps to meta. This method will recognize [TypedMetaItem], [Map]<String,Any?> and [List] of all mentioned above as value.
 * All other values will be converted to values.
 */
@DFExperimental
public fun Map<String, Any?>.toMeta(descriptor: NodeDescriptor? = null): Meta = Meta {
    @Suppress("UNCHECKED_CAST")
    fun toItem(value: Any?): MetaItem = when (value) {
        is MetaItem -> value
        is Meta -> MetaItemNode(value)
        is Map<*, *> -> MetaItemNode((value as Map<String, Any?>).toMeta())
        else -> MetaItemValue(Value.of(value))
    }

    entries.forEach { (key, value) ->
        if (value is List<*>) {
            val items = value.map { toItem(it) }
            if (items.all { it is MetaItemValue }) {
                set(key, ListValue(items.map { it.value!! }))
            } else {
                setIndexedItems(key.toName(), value.map { toItem(it) })
            }
        } else {
            set(key, toItem(value))
        }
    }
}