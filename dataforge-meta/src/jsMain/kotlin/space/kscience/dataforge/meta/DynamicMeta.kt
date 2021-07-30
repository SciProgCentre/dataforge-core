package space.kscience.dataforge.meta

import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.asValue
import space.kscience.dataforge.values.isList


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

public class DynamicMeta(internal val obj: dynamic) : Meta {
    private fun keys(): Array<String> = js("Object").keys(obj)

    private fun isArray(@Suppress("UNUSED_PARAMETER") obj: dynamic): Boolean =
        js("Array").isArray(obj) as Boolean

    private fun isPrimitive(obj: dynamic): Boolean =
        (jsTypeOf(obj) != "object")

    @Suppress("USELESS_CAST")
    override val value: Value?
        get() = if (isArray(obj) && (obj as Array<Any?>).all { isPrimitive(it) }) Value.of(obj as Array<Any?>)
        else when (jsTypeOf(obj)) {
            "boolean" -> (obj as Boolean).asValue()
            "number" -> (obj as Number).asValue()
            "string" -> (obj as String).asValue()
            else -> null
        }

    override val items: Map<NameToken, Meta>
        get() = keys().flatMap<String, Pair<NameToken, Meta>> { key ->
            val value = obj[key] ?: return@flatMap emptyList()
            when {
                isArray(value) -> {
                    val array = value as Array<Any?>
                    if (array.all { isPrimitive(it) }) {
                        emptyList()
                    } else {
                        array.mapIndexedNotNull { index, it ->
                            val item = DynamicMeta(it)
                            NameToken(key, index.toString()) to item
                        }
                    }
                }
                isPrimitive(obj) -> {
                    emptyList()
                }
                else -> {
                    val item = DynamicMeta(value)
                    listOf(NameToken(key) to item)
                }
            }
        }.toMap()

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}