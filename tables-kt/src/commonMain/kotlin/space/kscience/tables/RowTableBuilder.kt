package space.kscience.tables

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.Value
import space.kscience.dataforge.meta.ValueType
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public class RowTableBuilder<C>(
    override val rows: MutableList<Row<C>>,
    override val headers: MutableList<ColumnHeader<C>>,
) : RowTable<C>(headers, rows) {

    public fun <T : C> addColumn(header: ColumnHeader<C>) {
        headers.add(header)
    }

    /**
     * Create a new column header for the table
     */
    @PublishedApi
    internal fun <T : C> newColumn(name: String, type: KType, meta: Meta, index: Int?): ColumnHeader<T> {
        val header = SimpleColumnHeader<T>(name, type, meta)
        if (index == null) {
            headers.add(header)
        } else {
            headers.add(index, header)
        }
        return header
    }

    public fun addRow(row: Row<C>): Row<C> {
        rows.add(row)
        return row
    }

    public inline fun <reified T : C> newColumn(
        name: String,
        index: Int? = null,
        noinline columnMetaBuilder: ColumnScheme.() -> Unit = {},
    ): ColumnHeader<T> = newColumn(name, typeOf<T>(), ColumnScheme(columnMetaBuilder).toMeta(), index)

    public inline fun <reified T : C> column(
        index: Int? = null,
        noinline columnMetaBuilder: ColumnScheme.() -> Unit = {},
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ColumnHeader<T>>> =
        PropertyDelegateProvider { _, property ->
            val res: ColumnHeader<T> = newColumn(property.name, index, columnMetaBuilder)
            ReadOnlyProperty { _, _ -> res }
        }

    public fun row(map: Map<String, C?>): Row<C> {
        val row = MapRow(map)
        rows.add(row)
        return row
    }

    public fun <T : C> row(vararg pairs: Pair<ColumnHeader<T>, T>): Row<C> = addRow(Row(*pairs))
}

public fun RowTableBuilder<in Value>.newColumn(
    name: String,
    valueType: ValueType,
    index: Int? = null,
    columnMetaBuilder: ValueColumnScheme.() -> Unit = {},
): ColumnHeader<Value> = newColumn(
    name,
    typeOf<Value>(),
    ValueColumnScheme(columnMetaBuilder).also { it.valueType = valueType }.toMeta(),
    index
)

public fun RowTableBuilder<in Value>.column(
    valueType: ValueType,
    index: Int? = null,
    columnMetaBuilder: ValueColumnScheme.() -> Unit = {},
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ColumnHeader<Value>>> =
    PropertyDelegateProvider { _, property ->
        val res = newColumn(property.name, valueType, index, columnMetaBuilder)
        ReadOnlyProperty { _, _ -> res }
    }

public fun RowTableBuilder<Value>.valueRow(vararg pairs: Pair<ColumnHeader<Value>, Any?>): Row<Value> =
    row(pairs.associate { it.first.name to Value.of(it.second) })

/**
 * Add a row represented by Meta
 */
public fun RowTableBuilder<Value>.row(meta: Meta): Row<Value> {
    val row = MetaRow(meta)
    rows.add(row)
    return row
}

/**
 * Shallow copy table to a new [RowTableBuilder]
 */
public fun <T> RowTable<T>.toMutableRowTable(): RowTableBuilder<T> =
    RowTableBuilder(rows.toMutableList(), headers.toMutableList())

/**
 * Shallow copy and edit [Table] and edit it as [RowTable]
 */
public fun <T> Table<T>.withRows(block: RowTableBuilder<T>.() -> Unit): RowTable<T> =
    RowTableBuilder(rows.toMutableList(), headers.toMutableList()).apply(block)