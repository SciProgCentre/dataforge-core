package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass


class RowTable<R : Row>(override val rows: List<R>, override val header: TableHeader) : Table {
    override fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T? =
        rows[row].getValue(column, type)

    override val columns: List<Column<*>> get() = header.map { RotTableColumn(it) }

    private inner class RotTableColumn<T : Any>(val header: ColumnHeader<T>) : Column<T> {
        override val name: String get() = header.name
        override val type: KClass<out T> get() = header.type
        override val meta: Meta get() = header.meta
        override val size: Int get() = rows.size

        override fun get(index: Int): T? = rows[index].getValue(name, type)
    }

}

