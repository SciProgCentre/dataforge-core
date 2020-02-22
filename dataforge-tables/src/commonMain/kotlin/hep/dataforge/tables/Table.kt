package hep.dataforge.tables

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
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

/**
 * Finite or infinite row set. Rows are produced in a lazy suspendable [Flow].
 * Each row must contain at least all the fields mentioned in [header].
 */
interface Rows<C : Any> {
    val header: TableHeader<C>
    fun rowFlow(): Flow<Row<C>>
}

interface Table<C : Any> : Rows<C> {
    fun <T : C> getValue(row: Int, column: String, type: KClass<out T>): T?
    val columns: Collection<Column<C>>
    override val header: TableHeader<C> get() = columns.toList()
    val rows: List<Row<C>>
    override fun rowFlow(): Flow<Row<C>> = rows.asFlow()

    /**
     * Apply typed query to this table and return lazy [Flow] of resulting rows. The flow could be empty.
     */
    //fun select(query: Any): Flow<Row> = error("Query of type ${query::class} is not supported by this table")
    companion object {
        inline operator fun <T : Any> invoke(block: MutableTable<T>.() -> Unit): Table<T> =
            MutableTable<T>(arrayListOf(), arrayListOf()).apply(block)
    }
}

operator fun Collection<Column<*>>.get(name: String): Column<*>? = find { it.name == name }

inline operator fun <C : Any, reified T : C> Table<C>.get(row: Int, column: String): T? =
    getValue(row, column, T::class)

operator fun <C : Any, T : C> Table<C>.get(row: Int, column: ColumnHeader<T>): T? = getValue(row, column.name, column.type)

interface Column<T : Any> : ColumnHeader<T> {
    val size: Int
    operator fun get(index: Int): T?
}

val Column<*>.indices get() = (0 until size)

operator fun <T : Any> Column<T>.iterator() = iterator {
    for (i in indices) {
        yield(get(i))
    }
}

interface Row<C: Any> {
    fun <T : C> getValue(column: String, type: KClass<out T>): T?
}

inline operator fun <C : Any, reified T : C> Row<C>.get(column: String): T? = getValue(column, T::class)
operator fun <C : Any, T : C> Row<C>.get(column: ColumnHeader<T>): T? = getValue(column.name, column.type)