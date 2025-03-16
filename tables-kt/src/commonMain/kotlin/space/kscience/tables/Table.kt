package space.kscience.tables

import kotlinx.coroutines.flow.Flow

/**
 * Finite or infinite row set. Rows are produced in a lazy suspendable [Flow].
 * Each row must contain at least all the fields mentioned in [headers].
 */
public interface Rows<out T> {
    /**
     * An ordered list of headers that *must* be present.
     */
    public val headers: TableHeader<T>

    /**
     * A lazy sequence of rows.
     */
    public fun rowSequence(): Sequence<Row<T>>
}

/**
 * A basic abstraction for a table. The abstraction does not specify how the data is stored.
 */
public interface Table<out T> : Rows<T> {
    public fun getOrNull(row: Int, column: String): T?
    public val columns: Collection<Column<T>>
    override val headers: TableHeader<T> get() = columns.toList()
    public val rows: List<Row<T>>
    override fun rowSequence(): Sequence<Row<T>> = rows.asSequence()

    public companion object
}

public operator fun <T> Table<T>.get(row: Int, column: String): T =
    getOrNull(row, column) ?: error("Element with column $column and row $row not found in $this")

public fun <T> Collection<Column<T>>.getOrNull(name: String): Column<T>? = find { it.name == name }

public operator fun <T> Collection<Column<T>>.get(name: String): Column<T> = first { it.name == name }

public inline operator fun <T, reified R : T> Table<T>.get(row: Int, column: ColumnHeader<R>): R? {
    //require(headers.contains(column)) { "Column $column is not in table headers" }
    return getOrNull(row, column.name) as? R
}

public interface Column<out T> : ColumnHeader<T> {
    public val size: Int

    public fun getOrNull(index: Int): T?
}

public operator fun <T> Column<T>.get(index: Int): T =
    getOrNull(index) ?: error("Element with index $index not found in $this")

public inline fun <T> Column<T>.contentEquals(
    other: Column<T>,
    criterion: (l: T?, r: T?) -> Boolean = { l, r -> l == r },
): Boolean = this.indices == other.indices && indices.all { criterion(getOrNull(it), other.getOrNull(it)) }

public val Column<*>.indices: IntRange get() = (0 until size)

public operator fun <T> Column<T>.iterator(): Iterator<T?> = iterator {
    for (i in indices) {
        yield(getOrNull(i))
    }
}

public fun <T> Column<T>.sequence(): Sequence<T?> = sequence {
    for (i in indices) {
        yield(getOrNull(i))
    }
}

public fun <T> Column<T>.listValues(): List<T?> = if (this is ListColumn) {
    this.data
} else {
    sequence().toList()
}

/**
 * A common abstraction for a row of data. Fields could be accessed by its name
 */
public interface Row<out T> {
    /**
     * Get a value in [Row] by name or return null if value is missing.
     *
     * Note that null value for nullable [T] is indistinguishable from missing value.
     */
    public fun getOrNull(column: String): T?
}

/**
 * Get a [Row] value or throw error if it is null
 */
public operator fun <T: Any> Row<T>.get(column: String): T =
    getOrNull(column) ?: error("Element with column name $column not found in $this")

/**
 * Get a value by a column header and cast it if possible to a column type
 */
public inline operator fun <T, reified R : T> Row<T>.get(column: ColumnHeader<R>): R = getOrNull(column.name) as? R
    ?: error("Type conversion to ${R::class} failed for ${getOrNull(column.name)}")