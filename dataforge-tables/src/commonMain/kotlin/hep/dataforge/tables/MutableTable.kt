package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

class SimpleColumnHeader<T : Any>(
    override val name: String,
    override val type: KClass<out T>,
    override val meta: Meta
) : ColumnHeader<T>

class MutableTable<C : Any>(
    override val rows: MutableList<Row>,
    override val header: MutableList<ColumnHeader<C>>
) : RowTable<C, Row>(rows, header) {
    fun <T : C> column(name: String, type: KClass<out T>, meta: Meta): ColumnHeader<T> {
        val column = SimpleColumnHeader(name, type, meta)
        header.add(column)
        return column
    }

    inline fun <reified T : C> column(
        name: String,
        noinline columnMetaBuilder: ColumnScheme.() -> Unit
    ): ColumnHeader<T> {
        return column(name, T::class, ColumnScheme(columnMetaBuilder).toMeta())
    }

    fun row(block: MutableMap<String, Any?>.() -> Unit): Row {
        val map = HashMap<String, Any?>().apply(block)
        val row = MapRow(map)
        rows.add(row)
        return row
    }

    operator fun <T : Any> MutableMap<String, Any?>.set(header: ColumnHeader<T>, value: T?) {
        set(header.name, value)
    }
}

fun <C : Any> Table<C>.edit(block: MutableTable<C>.() -> Unit): Table<C> {
    return MutableTable(rows.toMutableList(), header.toMutableList()).apply(block)
}