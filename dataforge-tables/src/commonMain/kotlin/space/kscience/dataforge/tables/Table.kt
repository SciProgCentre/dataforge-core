package space.kscience.dataforge.tables

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Finite or infinite row set. Rows are produced in a lazy suspendable [Flow].
 * Each row must contain at least all the fields mentioned in [headers].
 */
public interface Rows<out T : Any> {
    public val headers: TableHeader<T>
    public fun rowFlow(): Flow<Row<T>>
}

public interface Table<out T : Any> : Rows<T> {
    public operator fun get(row: Int, column: String): T?
    public val columns: Collection<Column<T>>
    override val headers: TableHeader<T> get() = columns.toList()
    public val rows: List<Row<T>>
    override fun rowFlow(): Flow<Row<T>> = rows.asFlow()

    /**
     * Apply typed query to this table and return lazy [Flow] of resulting rows. The flow could be empty.
     */
    //fun select(query: Any): Flow<Row> = error("Query of type ${query::class} is not supported by this table")
    public companion object {
        public inline operator fun <T : Any> invoke(block: MutableTable<T>.() -> Unit): Table<T> =
            MutableTable<T>(arrayListOf(), arrayListOf()).apply(block)
    }
}

public operator fun <T : Any> Collection<Column<T>>.get(name: String): Column<T>? = find { it.name == name }


public inline operator fun <T : Any, reified R : T> Table<T>.get(row: Int, column: ColumnHeader<R>): R? {
    require(headers.contains(column)) { "Column $column is not in table headers" }
    return get(row, column.name) as? R
}

public interface Column<out T : Any> : ColumnHeader<T> {
    public val size: Int
    public operator fun get(index: Int): T?
}

public val Column<*>.indices: IntRange get() = (0 until size)

public operator fun <T : Any> Column<T>.iterator(): Iterator<T?> = iterator {
    for (i in indices) {
        yield(get(i))
    }
}

public interface Row<out T : Any> {
    public operator fun get(column: String): T?
}

public inline operator fun <T : Any, reified R : T> Row<T>.get(column: ColumnHeader<R>): R? = get(column.name) as? R