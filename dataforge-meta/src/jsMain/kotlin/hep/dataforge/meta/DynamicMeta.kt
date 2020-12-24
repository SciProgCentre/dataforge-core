package hep.dataforge.meta

import hep.dataforge.names.NameToken
import hep.dataforge.values.Null
import hep.dataforge.values.Value
import hep.dataforge.values.isList


//TODO add Meta wrapper for dynamic

public fun Value.toDynamic(): dynamic {
    return if (isList()) {
        list.map { it.toDynamic() }.toTypedArray().asDynamic()
    } else {
        value.asDynamic()
    }
}

/**
 * Represent or copy this [Meta] to dynamic object to be passed to JS libraries
 */
public fun Meta.toDynamic(): dynamic {
    if (this is DynamicMeta) return this.obj

    fun MetaItem.toDynamic(): dynamic = when (this) {
        is ValueItem -> this.value.toDynamic()
        is NodeItem -> this.node.toDynamic()
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

public class DynamicMeta(internal val obj: dynamic) : MetaBase() {
    private fun keys(): Array<String> = js("Object").keys(obj)

    private fun isArray(@Suppress("UNUSED_PARAMETER") obj: dynamic): Boolean =
        js("Array.isArray(obj)") as Boolean

    private fun isPrimitive(obj: dynamic): Boolean =
        (jsTypeOf(obj) != "object")

    @Suppress("UNCHECKED_CAST", "USELESS_CAST")
    private fun asItem(obj: dynamic): TypedMetaItem<DynamicMeta>? {
        return when {
            obj == null -> ValueItem(Null)
            isArray(obj) && (obj as Array<Any?>).all { isPrimitive(it) } -> ValueItem(Value.of(obj as Array<Any?>))
            else -> when (jsTypeOf(obj)) {
                "boolean" -> ValueItem(Value.of(obj as Boolean))
                "number" -> ValueItem(Value.of(obj as Number))
                "string" -> ValueItem(Value.of(obj as String))
                "object" -> NodeItem(DynamicMeta(obj))
                else -> null
            }
        }
    }

    override val items: Map<NameToken, TypedMetaItem<DynamicMeta>>
        get() = keys().flatMap<String, Pair<NameToken, TypedMetaItem<DynamicMeta>>> { key ->
            val value = obj[key] ?: return@flatMap emptyList()
            if (isArray(value)) {
                val array = value as Array<Any?>
                return@flatMap if (array.all { isPrimitive(it) }) {
                    listOf(NameToken(key) to ValueItem(Value.of(array)))
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