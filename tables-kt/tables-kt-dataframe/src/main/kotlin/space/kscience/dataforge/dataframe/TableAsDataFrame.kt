package space.kscience.dataforge.dataframe

import org.jetbrains.kotlinx.dataframe.AnyCol
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.columns.ColumnKind
import org.jetbrains.kotlinx.dataframe.columns.ValueColumn
import org.jetbrains.kotlinx.dataframe.indices
import space.kscience.tables.Table
import space.kscience.tables.get
import space.kscience.tables.indices
import kotlin.reflect.KType
import space.kscience.tables.Column as TableColumn

internal class ColumnAsDataColumn<T>(
    val column: TableColumn<T>,
    val indexList: List<Int> = column.indices.toList(),
    val nameOverride: String = column.name,
) : ValueColumn<T> {

    override fun get(indices: Iterable<Int>): ValueColumn<T> {
        val newIndices = indices.map { indexList[it] }
        return ColumnAsDataColumn<T>(column, newIndices, nameOverride)
    }

    override fun get(range: IntRange): ValueColumn<T> {
        val newIndices = indices.map { indexList[it] }
        return ColumnAsDataColumn<T>(column, newIndices, nameOverride)
    }

    override fun rename(newName: String): ValueColumn<T> = ColumnAsDataColumn<T>(column, indexList, newName)

    override fun distinct(): ValueColumn<T> {
        val newIndices = indexList.distinctBy { column.getOrNull(it) }
        return ColumnAsDataColumn<T>(column, newIndices, nameOverride)
    }

    override fun contains(value: T): Boolean = indexList.any { column.getOrNull(it) == value }

    override fun countDistinct(): Int = distinct().count()

    override fun defaultValue(): T? = null

    override fun get(index: Int): T = column[indexList[index]]

    override fun get(columnName: String): AnyCol =
        if (columnName == nameOverride) this else error("Sub-columns are not allowed")

    override fun kind(): ColumnKind = ColumnKind.Value

    override fun size(): Int = indexList.size

    override fun toSet(): Set<T> = indexList.map { column[it] }.toSet()

    override fun type(): KType = column.type

    override fun values(): Iterable<T> = indexList.asSequence().map { column[it] }.asIterable()

    override fun name(): String = nameOverride
}

internal fun <T> TableColumn<T>.asDataColumn(): AnyCol = if (this is DataColumnAsColumn) {
    this.column
} else {
    ColumnAsDataColumn(this)
}

//TODO convert typed value columns to primitive columns

@Suppress("UNCHECKED_CAST")
public fun <T> Table<T>.toDataFrame(): DataFrame<T> =
    dataFrameOf(columns.map { it.asDataColumn() }) as DataFrame<T>
