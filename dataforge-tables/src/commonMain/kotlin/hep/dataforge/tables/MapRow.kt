package hep.dataforge.tables

import kotlin.reflect.KClass

class MapRow(val values: Map<String, Any>) : Row {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(column: String, type: KClass<out T>): T? {
        val value = values[column]
        return when {
            value == null -> null
            type.isInstance(value) -> {
                value as T?
            }
            else -> {
                error("Expected type is $type, but found ${value::class}")
            }
        }
    }
}