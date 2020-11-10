package hep.dataforge.tables

import hep.dataforge.meta.Meta
import hep.dataforge.meta.invoke
import hep.dataforge.values.Value
import kotlin.reflect.KClass

public class MutableTable<C : Any>(
    override val rows: MutableList<Row<C>>,
    override val header: MutableList<ColumnHeader<C>>
) : RowTable<C>(rows, header) {

    public fun <R : C> column(name: String, type: KClass<out R>, meta: Meta): ColumnHeader<R> {
        val column = SimpleColumnHeader(name, type, meta)
        header.add(column)
        return column
    }

    public inline fun <reified T : C> column(
        name: String,
        noinline columnMetaBuilder: ColumnScheme.() -> Unit = {}
    ): ColumnHeader<T> {
        return column(name, T::class, ColumnScheme(columnMetaBuilder).toMeta())
    }

    public fun row(map: Map<String, C?>): Row<C> {
        val row = MapRow(map)
        rows.add(row)
        return row
    }

    public fun <T : C> row(vararg pairs: Pair<ColumnHeader<T>, T>): Row<C> =
        row(pairs.associate { it.first.name to it.second })
}

public fun MutableTable<Value>.row(vararg pairs: Pair<ColumnHeader<Value>, Any?>): Row<Value> =
    row(pairs.associate { it.first.name to Value.of(it.second) })

public fun <C : Any> Table<C>.edit(block: MutableTable<C>.() -> Unit): Table<C> {
    return MutableTable(rows.toMutableList(), header.toMutableList()).apply(block)
}