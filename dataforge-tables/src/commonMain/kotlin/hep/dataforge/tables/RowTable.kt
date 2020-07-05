package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KClass

inline class MapRow<C : Any>(val values: Map<String, C?>) : Row<C> {
    override fun getValue(column: String): C? = values[column]
}

internal class RowTableColumn<C : Any, T : C>(val table: Table<C>, val header: ColumnHeader<T>) : Column<T> {
    override val name: String get() = header.name
    override val type: KClass<out T> get() = header.type
    override val meta: Meta get() = header.meta
    override val size: Int get() = table.rows.size

    override fun get(index: Int): T? = table.rows[index].getValue(name, type)
}

open class RowTable<C : Any>(override val rows: List<Row<C>>, override val header: List<ColumnHeader<C>>) : Table<C> {
    override fun getValue(row: Int, column: String): C? = rows[row].getValue(column)

    override val columns: List<Column<C>> get() = header.map { RowTableColumn(this, it) }
}

suspend fun <C : Any> Rows<C>.collect(): Table<C> = this as? Table<C> ?: RowTable(rowFlow().toList(), header)