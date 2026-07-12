package space.kscience.dataforge.dataframe

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import space.kscience.dataforge.meta.Meta
import space.kscience.tables.Column
import space.kscience.tables.ColumnHeader
import space.kscience.tables.Row
import space.kscience.tables.Table
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf

@JvmInline
internal value class DataColumnAsColumn<T>(val column: DataColumn<T>) : Column<T> {
    override val name: String get() = column.name
    override val meta: Meta get() = Meta.EMPTY
    override val type: KType get() = column.type
    override val size: Int get() = column.size

    override fun getOrNull(index: Int): T = column[index]
}

internal fun <T> DataColumn<T>.toTableColumn(): Column<T> = if (this is ColumnAsDataColumn) {
    this.column
} else {
    DataColumnAsColumn(this)
}

@JvmInline
private value class DataRowAsRow<T>(val row: DataRow<T>) : Row<T> {
    @Suppress("UNCHECKED_CAST")
    override fun getOrNull(column: String): T? = row[column] as? T
}

@JvmInline
internal value class DataFrameAsTable<T>(private val dataFrame: DataFrame<T>) : Table<T> {

    @Suppress("UNCHECKED_CAST")
    override fun getOrNull(row: Int, column: String): T? = dataFrame.getColumn(column)[row] as? T

    override val columns: Collection<Column<T>>
        get() = dataFrame.columns().map { it.cast<T>().toTableColumn() }

    override val rows: List<Row<T>>
        get() = dataFrame.rows().map { DataRowAsRow(it) }
}

/**
 * Represent a [DataFrame] as a [Table]
 */
public fun <T> DataFrame<T>.asTable(): Table<T> = DataFrameAsTable(this)

/**
 * Retrieves a column from the current [DataFrame] based on the specified [ColumnHeader].
 *
 * This function looks up the column in the [DataFrame] using the name provided
 * in the given [ColumnHeader]. The resulting column is returned as a [DataColumn].
 *
 * @param R The type of the column's values, inferred from the [ColumnHeader].
 * @param header The [ColumnHeader] defining the name and type of the column to retrieve.
 * @return The [DataColumn] corresponding to the specified [ColumnHeader].
 */
public operator fun <R> DataFrame<*>.get(header: ColumnHeader<R>): DataColumn<R> {
    val index = columnNames().indexOf(header.name)

    if (index == -1) throw NoSuchElementException("Column ${header.name} not found in row")

    val columnType = columnTypes()[index]

    if (header.type.isSupertypeOf(columnType)) {
        @Suppress("UNCHECKED_CAST")
        return getColumn(index).cast<R>()
    } else {
        error("Column type mismatch for ${header.name}. Expected ${header.type}, but found ${columnType}")
    }
}

/**
 * Retrieves the value of the specified column from the current data row.
 * The column is identified by its header, which includes the column name and type.
 *
 * @param header The header of the column to retrieve. Includes the column's name and type information.
 * @return The value of the specified column from the current row, cast to the type specified in the header.
 * @throws NoSuchElementException If the column with the specified name does not exist in the row.
 * @throws IllegalStateException If the type of the column in the row does not match the type specified in the header.
 */
public operator fun <R> DataRow<*>.get(header: ColumnHeader<R>): R {
    val index = columnNames().indexOf(header.name)

    if (index == -1) throw NoSuchElementException("Column ${header.name} not found in row")

    val columnType = columnTypes()[index]

    if (header.type.isSupertypeOf(columnType)) {
        @Suppress("UNCHECKED_CAST")
        return get(index) as R
    } else {
        error("Column type mismatch for ${header.name}. Expected ${header.type}, but found ${columnType}")
    }
}
