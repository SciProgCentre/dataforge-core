package hep.dataforge.meta

import hep.dataforge.names.NameToken
import hep.dataforge.values.Null
import hep.dataforge.values.Value
import hep.dataforge.values.isList


//TODO add Meta wrapper for dynamic

fun Value.toDynamic(): dynamic {
    return if (isList()) {
        list.map { it.toDynamic() }.toTypedArray().asDynamic()
    } else {
        value.asDynamic()
    }
}

/**
 * Represent or copy this [Meta] to dynamic object to be passed to JS libraries
 */
fun Meta.toDynamic(): dynamic {
    if (this is DynamicMeta) return this.obj

    fun MetaItem<*>.toDynamic(): dynamic = when (this) {
        is MetaItem.ValueItem -> this.value.toDynamic()
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

class DynamicMeta(val obj: dynamic) : MetaBase() {
    private fun keys() = js("Object.keys(this.obj)") as Array<String>

    private fun isArray(@Suppress("UNUSED_PARAMETER") obj: dynamic): Boolean =
        js("Array.isArray(obj)") as Boolean

    private fun isPrimitive(obj: dynamic): Boolean =
        (jsTypeOf(obj) != "object")

    @Suppress("UNCHECKED_CAST")
    private fun asItem(obj: dynamic): MetaItem<DynamicMeta>? {
        return when {
            obj == null -> MetaItem.ValueItem(Null)
            isArray(obj) && (obj as Array<Any?>).all { isPrimitive(it) } -> MetaItem.ValueItem(Value.of(obj as Array<Any?>))
            else -> when (jsTypeOf(obj)) {
                "boolean" -> MetaItem.ValueItem(Value.of(obj as Boolean))
                "number" -> MetaItem.ValueItem(Value.of(obj as Number))
                "string" -> MetaItem.ValueItem(Value.of(obj as String))
                "object" -> MetaItem.NodeItem(DynamicMeta(obj))
                else -> null
            }
        }
    }

    override val items: Map<NameToken, MetaItem<DynamicMeta>>
        get() = keys().flatMap<String, Pair<NameToken, MetaItem<DynamicMeta>>> { key ->
            val value = obj[key] ?: return@flatMap emptyList()
            if (isArray(value)) {
                val array = value as Array<Any?>
                return@flatMap if (array.all { isPrimitive(it) }) {
                    listOf(NameToken(key) to MetaItem.ValueItem(Value.of(array)))
                } else {
                    array.mapIndexedNotNull { index, it ->
                        val item = asItem(it) ?: return@mapIndexedNotNull null
                        NameToken(key, index.toString()) to item
                    }
                }
            } else {
                val item = asItem(value) ?: return@flatMap emptyList()
                listOf(NameToken(key) to item)
            }
        }.associate { it }
}