package hep.dataforge.meta

import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.values.Value

///**
// * Find all elements with given body
// */
//private fun Meta.byBody(body: String): Map<String, MetaItem<*>> =
//    items.filter { it.key.body == body }.mapKeys { it.key.index }
//
//private fun Meta.distinctNames() = items.keys.map { it.body }.distinct()

/**
 * Convert meta to map of maps
 */
@DFExperimental
fun Meta.toMap(descriptor: NodeDescriptor? = null): Map<String, Any?> {
    return items.entries.associate { (token, item) ->
        token.toString() to when (item) {
            is MetaItem.NodeItem -> item.node.toMap()
            is MetaItem.ValueItem -> item.value.value
        }
    }
}

/**
 * Convert map of maps to meta
 */
@DFExperimental
fun Map<String, Any?>.toMeta(descriptor: NodeDescriptor? = null): Meta = buildMeta {
    entries.forEach { (key, value) ->
        @Suppress("UNCHECKED_CAST")
        when (value) {
            is Map<*, *> -> setNode(key, (value as Map<String, Any?>).toMeta())
            else -> setValue(key, Value.of(value))
        }
    }
}