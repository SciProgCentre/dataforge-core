package hep.dataforge.tables

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Finite or infinite row set. Rows are produced in a lazy suspendable [Flow].
 * Each row must contain at least all the fields mentioned in [header].
 */
interface Rows<out T : Any> {
    val header: TableHeader<T>
    fun rowFlow(): Flow<Row<T>>
}

interface Table<out T : Any> : Rows<T> {
    fun getValue(row: Int, column: String): T?
    val columns: Collection<Column<T>>
    override val header: TableHeader<T> get() = columns.toList()
    val rows: List<Row<T>>
    override fun rowFlow(): Flow<Row<T>> = rows.asFlow()

    /**
     * Apply typed query to this table and return lazy [Flow] of resulting rows. The flow could be empty.
     */
    //fun select(query: Any): Flow<Row> = error("Query of type ${query::class} is not supported by this table")
    companion object {
        inline operator fun <T : Any> invoke(block: MutableTable<T>.() -> Unit): Table<T> =
            MutableTable<T>(arrayListOf(), arrayListOf()).apply(block)
    }
}

fun <C : Any, T : C> Table<C>.getValue(row: Int, column: String, type: KClass<out T>): T? =
    type.cast(getValue(row, column))

operator fun <T : Any> Collection<Column<T>>.get(name: String): Column<T>? = find { it.name == name }

inline operator fun <C : Any, reified T : C> Table<C>.get(row: Int, column: String): T? =
    getValue(row, column, T::class)

operator fun <C : Any, T : C> Table<C>.get(row: Int, column: ColumnHeader<T>): T? =
    getValue(row, column.name, column.type)

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

interface Row<out T : Any> {
    fun getValue(column: String): T?
}

fun <C : Any, T : C> Row<C>.getValue(column: String, type: KClass<out T>): T? = type.cast(getValue(column))

inline operator fun <reified T : Any> Row<T>.get(column: String): T? = T::class.cast(getValue(column))
operator fun <C : Any, T : C> Row<C>.get(column: ColumnHeader<T>): T? = getValue(column.name, column.type)