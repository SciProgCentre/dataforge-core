package space.kscience.dataforge.tables

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.values.Value
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public class MutableTable<C : Any>(
    override val rows: MutableList<Row<C>>,
    override val headers: MutableList<ColumnHeader<C>>,
) : RowTable<C>(rows, headers) {

    @PublishedApi
    internal fun <T : C> addColumn(name: String, type: KType, meta: Meta): ColumnHeader<T> {
        val column = SimpleColumnHeader<T>(name, type, meta)
        headers.add(column)
        return column
    }

    public inline fun <reified T : C> addColumn(
        name: String,
        noinline columnMetaBuilder: ColumnScheme.() -> Unit = {},
    ): ColumnHeader<T> = addColumn(name, typeOf<T>(), ColumnScheme(columnMetaBuilder).toMeta())

    public inline fun <reified T : C> column(
        noinline columnMetaBuilder: ColumnScheme.() -> Unit = {}
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ColumnHeader<T>>> =
        PropertyDelegateProvider { _, property ->
            val res = addColumn<T>(property.name, columnMetaBuilder)
            ReadOnlyProperty { _, _ -> res }
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
    return MutableTable(rows.toMutableList(), headers.toMutableList()).apply(block)
}