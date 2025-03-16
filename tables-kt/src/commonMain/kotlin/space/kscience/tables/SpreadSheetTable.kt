package space.kscience.tables

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.Value
import kotlin.jvm.JvmName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private fun cellId(row: Int, column: String) = "$column[$row]"


public data class SpreadSheetCell<T>(val row: Int, val column: String, val value: T) {
    val id: String get() = cellId(row, column)
}

public class SpreadSheetTable<T>(
    private val cellValueType: KType,
    public val cellMap: MutableMap<String, SpreadSheetCell<T>> = HashMap(),
    public val columnDefs: MutableMap<String, ColumnHeader<T>> = HashMap(),
) : MutableTable<T> {

    public val cells: Collection<SpreadSheetCell<T>> get() = cellMap.values

    override fun getOrNull(row: Int, column: String): T? = cellMap[cellId(row, column)]?.value

    public override fun set(row: Int, column: String, value: T?) {
        val cellId = cellId(row, column)
        if (value == null) {
            cellMap.remove(cellId)
        } else {
            cellMap[cellId] = SpreadSheetCell(row, column, value)
        }
    }

    override val columns: Collection<Column<T>>
        get() = cells.groupBy(
            keySelector = { it.column },
            valueTransform = { it.value }
        ).entries.map { (key, values) ->
            val header = columnDefs[key] ?: SimpleColumnHeader(key, cellValueType, Meta.EMPTY)
            ListColumn(header, values)
        }


    override val rows: List<Row<T>>
        get() = cells.groupBy { it.row }.map {
            MapRow(it.value.associate { cell -> cell.column to cell.value })
        }

    public inline fun <reified R : T> defineColumn(name: String, schemeBuilder: ColumnScheme.() -> Unit) {
        columnDefs[name] = SimpleColumnHeader(name, typeOf<R>(), ColumnScheme(schemeBuilder).toMeta())
    }
}

public inline fun <reified T> SpreadSheetTable(builder: SpreadSheetTable<T>.() -> Unit): SpreadSheetTable<T> =
    SpreadSheetTable<T>(typeOf<T>()).apply(builder)

@JvmName("setValue")
public operator fun SpreadSheetTable<Value>.set(row: Int, column: ColumnHeader<Value>, value: Any?) {
    columnDefs[column.name] = column
    set(row, column.name, Value.of(value))
}

/**
 * Update values and header for given column
 */
public operator fun <T> SpreadSheetTable<T>.set(column: ColumnHeader<T>, values: List<T>) {
    columnDefs[column.name] = column
    values.forEachIndexed { index, value -> set(index, column, value) }
}

@JvmName("setValues")
public operator fun SpreadSheetTable<Value>.set(column: ColumnHeader<Value>, values: List<Any?>) {
    columnDefs[column.name] = column
    values.forEachIndexed { index, value -> set(index, column, value) }
}