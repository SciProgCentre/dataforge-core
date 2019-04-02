package hep.dataforge.meta

import hep.dataforge.names.NameToken
import hep.dataforge.values.Null
import hep.dataforge.values.Value


//TODO add Meta wrapper for dynamic

/**
 * Represent or copy this [Meta] to dynamic object to be passed to JS libraries
 */
fun Meta.toDynamic(): dynamic {
    if(this is DynamicMeta) return this.obj

    fun MetaItem<*>.toDynamic(): dynamic = when (this) {
        is MetaItem.ValueItem -> this.value.value.asDynamic()
        is MetaItem.NodeItem -> this.node.toDynamic()
    }

    val res = js("{}")
    this.items.entries.groupBy { it.key.body }.forEach { (key, value) ->
        val list = value.map { it.value }
        res[key] = when (list.size) {
            1 -> list.first().toDynamic()
            else -> list.map { it.toDynamic() }
        }
    }
    return res
}

class DynamicMeta(val obj: dynamic) : Meta {
    private fun keys() = js("Object.keys(this.obj)") as Array<String>

    private fun isArray(@Suppress("UNUSED_PARAMETER") obj: dynamic): Boolean =
        js("Array.isArray(obj)") as Boolean

    private fun asItem(obj: dynamic): MetaItem<DynamicMeta>? {
        if (obj == null) return MetaItem.ValueItem(Null)
        return when (jsTypeOf(obj)) {
            "boolean" -> MetaItem.ValueItem(Value.of(obj as Boolean))
            "number" -> MetaItem.ValueItem(Value.of(obj as Number))
            "string" -> MetaItem.ValueItem(Value.of(obj as String))
            "object" -> MetaItem.NodeItem(DynamicMeta(obj))
            else -> null
        }
    }

    override val items: Map<NameToken, MetaItem<DynamicMeta>>
        get() = keys().flatMap<String, Pair<NameToken, MetaItem<DynamicMeta>>> { key ->
            val value = obj[key] ?: return@flatMap emptyList()
            if (isArray(value)) {
                return@flatMap (value as Array<dynamic>)
                    .mapIndexedNotNull() { index, it ->
                        val item = asItem(it) ?: return@mapIndexedNotNull null
                        NameToken(key, index.toString()) to item
                    }
            } else {
                val item = asItem(value) ?: return@flatMap emptyList()
                listOf(NameToken(key) to item)
            }
        }.associate { it }
}