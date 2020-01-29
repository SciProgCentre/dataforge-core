package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.cast
import kotlin.reflect.full.isSubclassOf

class TableAccessor(val table: Table) : Table by table {
    inline fun <reified T : Any> column() = ColumnProperty(table, T::class)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Column<*>.cast(type: KClass<T>): Column<T> {
    return if (type.isSubclassOf(this.type)) {
        this as Column<T>
    } else {
        ColumnWrapper(this, type)
    }
}

class ColumnWrapper<T : Any>(val column: Column<*>, override val type: KClass<T>) : Column<T> {
    override val name: String get() = column.name
    override val meta: Meta get() = column.meta
    override val size: Int get() = column.size


    override fun get(index: Int): T? = type.cast(column[index])
}

class ColumnProperty<T : Any>(val table: Table, val type: KClass<T>) : ReadOnlyProperty<Any?, Column<T>?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Column<T>? {
        val name = property.name
        return table.columns[name]?.cast(type)
    }
}