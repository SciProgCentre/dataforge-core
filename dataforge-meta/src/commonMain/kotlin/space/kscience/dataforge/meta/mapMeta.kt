package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.toName
import space.kscience.dataforge.values.ListValue
import space.kscience.dataforge.values.Value

/**
 * Convert meta to map of maps
 */
public fun Meta.toMap(descriptor: MetaDescriptor? = null): Map<String, Any?> = buildMap {
    items.forEach { (key, child) ->
        if (child.items.isEmpty()) {
            //single-value meta is considered a leaf
            put(key.toString(), child.value)
        } else {
            //if child contains both children and value, then value will be placed into `@value` key
            put(key.toString(), child.toMap(descriptor?.get(key.body)))
        }
    }
    if (value != null) {
        put(Meta.VALUE_KEY, value)
    }
}


/**
 * Convert map of maps to meta. This method will recognize [Meta], [Map]<String,Any?> and [List] of all mentioned above as value.
 * All other values will be converted to values.
 */
@DFExperimental
public fun Map<String, Any?>.toMeta(descriptor: MetaDescriptor? = null): Meta = Meta {
    @Suppress("UNCHECKED_CAST")
    fun toMeta(value: Any?): Meta = when (value) {
        is Meta -> value
        is Map<*, *> -> (value as Map<String, Any?>).toMeta()
        else -> Meta(Value.of(value))
    }

    entries.forEach { (key, value) ->
        if (value is List<*>) {
            val items = value.map { toMeta(it) }
            if (items.all { it.isLeaf }) {
                set(key, ListValue(items.map { it.value!! }))
            } else {
                setIndexedItems(key.toName(), value.map { toMeta(it) })
            }
        } else {
            set(key, toMeta(value))
        }
    }
}