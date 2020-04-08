package hep.dataforge.meta

import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.names.toName
import hep.dataforge.values.ListValue
import hep.dataforge.values.Value

/**
 * Convert meta to map of maps
 */
fun Meta.toMap(descriptor: NodeDescriptor? = null): Map<String, Any?> {
    return items.entries.associate { (token, item) ->
        token.toString() to when (item) {
            is MetaItem.NodeItem -> item.node.toMap()
            is MetaItem.ValueItem -> item.value.value
        }
    }
}

/**
 * Convert map of maps to meta. This method will recognize [MetaItem], [Map]<String,Any?> and [List] of all mentioned above as value.
 * All other values will be converted to values.
 */
@DFExperimental
fun Map<String, Any?>.toMeta(descriptor: NodeDescriptor? = null): Meta = Meta {
    @Suppress("UNCHECKED_CAST")
    fun toItem(value: Any?): MetaItem<*> = when (value) {
        is MetaItem<*> -> value
        is Meta -> MetaItem.NodeItem(value)
        is Map<*, *> -> MetaItem.NodeItem((value as Map<String, Any?>).toMeta())
        else -> MetaItem.ValueItem(Value.of(value))
    }

    entries.forEach { (key, value) ->
        if (value is List<*>) {
            val items = value.map { toItem(it) }
            if (items.all { it is MetaItem.ValueItem }) {
                setValue(key, ListValue(items.map { it.value!! }))
            } else {
                setIndexedItems(key.toName(), value.map { toItem(it) })
            }
        } else {
            setItem(key, toItem(value))
        }
    }
}