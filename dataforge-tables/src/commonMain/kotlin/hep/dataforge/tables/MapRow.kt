package hep.dataforge.tables

import kotlin.reflect.KClass

inline class MapRow(val values: Map<String, Any?>) : Row {
    override fun <T : Any> getValue(column: String, type: KClass<out T>): T? {
        val value = values[column]
        return type.cast(value)
    }
}