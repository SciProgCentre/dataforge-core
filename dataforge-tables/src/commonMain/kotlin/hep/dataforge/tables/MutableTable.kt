package hep.dataforge.tables

import hep.dataforge.meta.Meta
import hep.dataforge.values.Value
import kotlin.reflect.KClass

class MutableTable<C : Any>(
    override val rows: MutableList<Row<C>>,
    override val header: MutableList<ColumnHeader<C>>
) : RowTable<C>(rows, header) {

    fun <T : C> column(name: String, type: KClass<out T>, meta: Meta): ColumnHeader<T> {
        val column = SimpleColumnHeader(name, type, meta)
        header.add(column)
        return column
    }

    inline fun <reified T : C> column(
        name: String,
        noinline columnMetaBuilder: ColumnScheme.() -> Unit = {}
    ): ColumnHeader<T> {
        return column(name, T::class, ColumnScheme(columnMetaBuilder).toMeta())
    }

    fun row(map: Map<String, C?>): Row<C> {
        val row = MapRow(map)
        rows.add(row)
        return row
    }

    fun <T : C> row(vararg pairs: Pair<ColumnHeader<T>, T>): Row<C> =
        row(pairs.associate { it.first.name to it.second })
}

fun MutableTable<Value>.row(vararg pairs: Pair<ColumnHeader<Value>, Any?>): Row<Value> =
    row(pairs.associate { it.first.name to Value.of(it.second) })

fun <C : Any> Table<C>.edit(block: MutableTable<C>.() -> Unit): Table<C> {
    return MutableTable(rows.toMutableList(), header.toMutableList()).apply(block)
}