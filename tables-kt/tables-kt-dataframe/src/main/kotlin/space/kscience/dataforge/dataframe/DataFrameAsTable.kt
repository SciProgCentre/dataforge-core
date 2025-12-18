package space.kscience.dataforge.dataframe

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.column
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.api.rows
import space.kscience.dataforge.meta.Meta
import space.kscience.tables.Column
import space.kscience.tables.ColumnHeader
import space.kscience.tables.Row
import space.kscience.tables.Table
import kotlin.reflect.KType

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

public operator fun <R> DataFrame<*>.get(header: ColumnHeader<R>): DataColumn<R> {
    val reference = column<R>(header.name)
    return get(reference)
}

public operator fun <R> DataRow<*>.get(header: ColumnHeader<R>): R {
    val reference = column<R>(header.name)
    return get(reference)
}
