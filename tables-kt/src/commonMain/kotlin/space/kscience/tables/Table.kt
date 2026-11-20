package space.kscience.tables

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

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

    /**
     * A lazy flow of rows.
     */
    public fun rowFlow(): Flow<Row<T>> = rowSequence().asFlow()
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
    override fun rowFlow(): Flow<Row<T>> = rows.asFlow()

    public companion object
}

public operator fun <T> Table<T>.get(row: Int, column: String): T =
    getOrNull(row, column) ?: error("Element with column $column and row $row not found in $this")

public fun <T> Collection<Column<T>>.getOrNull(name: String): Column<T>? = find { it.name == name }

/**
 * Returns the first column in this collection with the specified name.
 *
 * @param T the declared value type of the column
 * @param name the name of the column to find
 * @return the column whose name is equal to [name]
 * @throws NoSuchElementException if no column with the specified name is found
 */
public operator fun <T> Collection<Column<T>>.get(name: String): Column<T> = first { it.name == name }

/**
 * Returns the value stored at the given row and typed column.
 *
 * The lookup is performed by [ColumnHeader.name] and the resulting value is safely cast to [R].
 *
 * @param T upper bound type of values stored in the table.
 * @param R expected value type of the specified column.
 * @param row zero-based row index.
 * @param column typed header identifying the column to read.
 * @return the cell value cast to [R], or `null` if the value is absent, `null`, or not of type [R].
 */
public inline operator fun <T, reified R : T> Table<T>.get(row: Int, column: ColumnHeader<R>): R? {
    //require(headers.contains(column)) { "Column $column is not in table headers" }
    return getOrNull(row, column.name) as? R
}

/**
 * A typed column of tabular data with an associated [ColumnHeader].
 *
 * A column provides header information such as name, type, and metadata, and exposes
 * indexed access to its values. Individual values may be absent, in which case access
 * returns `null`.
 *
 * @param T the declared value type of the column
 */
public interface Column<out T> : ColumnHeader<T> {
    public val size: Int

    public fun getOrNull(index: Int): T?
}

/**
 * Returns the value in this column at the specified [index].
 *
 * @param T the type of values stored in the column.
 * @param index the zero-based index of the value to retrieve.
 * @return the value at the specified [index].
 * @throws IllegalStateException if no value is found at the specified [index].
 */
public operator fun <T> Column<T>.get(index: Int): T =
    getOrNull(index) ?: error("Element with index $index not found in $this")

/**
 * Checks whether this column has the same indices and matching content as [other].
 *
 * Two columns are considered content-equal when their index ranges are equal and
 * [criterion] returns `true` for every pair of values at the same index.
 * Column metadata, name, and type are not compared.
 *
 * @param T the type of values stored in the columns.
 * @param other the column to compare with this column.
 * @param criterion the predicate used to compare values at the same index.
 * @return `true` if both columns have the same indices and all corresponding values match according to [criterion], otherwise `false`.
 */
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

/**
 * Returns a lazy [Sequence] of this column's values in index order.
 *
 * Each element is obtained with [Column.getOrNull], so missing values are represented as `null`.
 *
 * @return a sequence containing values for all indices of this column, from first to last.
 */
public fun <T> Column<T>.asSequence(): Sequence<T?> = sequence {
    for (i in indices) {
        yield(getOrNull(i))
    }
}

/**
 * Returns this column's values as a [List] in index order.
 *
 * If this column is a [ListColumn], returns its backing data list directly.
 * Otherwise, materializes the values from [asSequence]. Missing values are represented as `null`.
 *
 * @return a list containing values for all indices of this column, from first to last.
 */
public fun <T> Column<T>.toList(): List<T?> = if (this is ListColumn) {
    this.data
} else {
    asSequence().toList()
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
public operator fun <T : Any> Row<T>.get(column: String): T =
    getOrNull(column) ?: error("Element with column name $column not found in $this")

/**
 * Get a value by a column header and cast it if possible to a column type
 */
public inline operator fun <T, reified R : T> Row<T>.get(column: ColumnHeader<R>): R = getOrNull(column.name) as? R
    ?: error("Type conversion to ${R::class} failed for ${getOrNull(column.name)}")