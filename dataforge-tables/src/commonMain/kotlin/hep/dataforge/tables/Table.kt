package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

//TODO to be removed in 1.3.70
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> KClass<T>.cast(value: Any?): T? {
    return when {
        value == null -> null
        !isInstance(value) -> error("Expected type is $this, but found ${value::class}")
        else -> value as T
    }
}

typealias TableHeader = List<ColumnHeader<*>>


interface Table<out C: Any> {
    fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T?
    val columns: Collection<Column<C>>
    val header: TableHeader get() = columns.toList()
    val rows: List<Row>

    /**
     * Apply query to a table and return lazy Flow
     */
    //fun find(query: Any): Flow<Row>
}

operator fun Collection<Column<*>>.get(name: String): Column<*>? = find { it.name == name }

inline operator fun <C: Any, reified T : C> Table<C>.get(row: Int, column: String): T? = getValue(row, column, T::class)

interface ColumnHeader<out T : Any> {
    val name: String
    val type: KClass<out T>
    val meta: Meta
}

operator fun <C: Any, T : C> Table<C>.get(row: Int, column: Column<T>): T? = getValue(row, column.name, column.type)

interface Column<out T : Any> : ColumnHeader<T> {
    val size: Int
    operator fun get(index: Int): T?
}

val Column<*>.indices get() = (0 until size)

operator fun <T : Any> Column<T>.iterator() = iterator {
    for (i in indices) {
        yield(get(i))
    }
}

interface Row {
    fun <T : Any> getValue(column: String, type: KClass<out T>): T?
}

inline operator fun <reified T : Any> Row.get(column: String): T? = getValue(column, T::class)
operator fun <T : Any> Row.get(column: Column<T>): T? = getValue(column.name, column.type)