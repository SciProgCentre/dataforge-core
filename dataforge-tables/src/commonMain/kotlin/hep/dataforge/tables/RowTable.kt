package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

data class ColumnDef<T : Any>(val name: String, val type: KClass<T>, val meta: Meta)

class RowTable<R : Row>(override val rows: List<R>, private val columnDefs: List<ColumnDef<*>>) : Table {
    override fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T? =
        rows[row].getValue(column, type)

    override val columns: Map<String, Column<*>>
        get() = columnDefs.associate { it.name to VirtualColumn(it) }

    private inner class VirtualColumn<T : Any>(val def: ColumnDef<T>) :
        Column<T> {

        override val name: String get() = def.name
        override val type: KClass<out T> get() = def.type
        override val meta: Meta get() = def.meta
        override val size: Int get() = rows.size

        override fun get(index: Int): T? = rows[index].getValue(name, type)
    }
}

