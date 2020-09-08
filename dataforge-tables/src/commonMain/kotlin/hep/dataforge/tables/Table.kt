package hep.dataforge.tables

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Finite or infinite row set. Rows are produced in a lazy suspendable [Flow].
 * Each row must contain at least all the fields mentioned in [header].
 */
public interface Rows<out T : Any> {
    public val header: TableHeader<T>
    public fun rowFlow(): Flow<Row<T>>
}

public interface Table<out T : Any> : Rows<T> {
    public fun getValue(row: Int, column: String): T?
    public val columns: Collection<Column<T>>
    override val header: TableHeader<T> get() = columns.toList()
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

public fun <C : Any, T : C> Table<C>.getValue(row: Int, column: String, type: KClass<out T>): T? =
    type.cast(getValue(row, column))

public operator fun <T : Any> Collection<Column<T>>.get(name: String): Column<T>? = find { it.name == name }

public inline operator fun <C : Any, reified T : C> Table<C>.get(row: Int, column: String): T? =
    getValue(row, column, T::class)

public operator fun <C : Any, T : C> Table<C>.get(row: Int, column: ColumnHeader<T>): T? =
    getValue(row, column.name, column.type)

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
    public fun getValue(column: String): T?
}

public fun <C : Any, T : C> Row<C>.getValue(column: String, type: KClass<out T>): T? = type.cast(getValue(column))

public inline operator fun <reified T : Any> Row<T>.get(column: String): T? = T::class.cast(getValue(column))
public operator fun <C : Any, T : C> Row<C>.get(column: ColumnHeader<T>): T? = getValue(column.name, column.type)