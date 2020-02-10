package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KClass

internal class RowTableColumn<C : Any, T : C>(val table: Table<C>, val header: ColumnHeader<T>) : Column<T> {
    override val name: String get() = header.name
    override val type: KClass<out T> get() = header.type
    override val meta: Meta get() = header.meta
    override val size: Int get() = table.rows.size

    override fun get(index: Int): T? = table.rows[index].getValue(name, type)
}

open class RowTable<C : Any, R : Row>(override val rows: List<R>, override val header: List<ColumnHeader<C>>) :
    Table<C> {
    override fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T? =
        rows[row].getValue(column, type)

    override val columns: List<Column<C>> get() = header.map { RowTableColumn(this, it) }
}

suspend fun Rows.collect(): Table<*> = this as? Table<*> ?: RowTable(rowFlow().toList(), header)