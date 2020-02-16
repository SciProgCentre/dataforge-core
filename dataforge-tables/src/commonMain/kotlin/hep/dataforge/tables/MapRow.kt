package hep.dataforge.tables

import kotlin.reflect.KClass

inline class MapRow<C: Any>(val values: Map<String, C?>) : Row<C> {
    override fun <T : C> getValue(column: String, type: KClass<out T>): T? {
        val value = values[column]
        return type.cast(value)
    }
}