package space.kscience.dataforge.tables

import space.kscience.dataforge.meta.Meta
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

@Suppress("UNCHECKED_CAST")
public fun <T : Any> Column<*>.cast(type: KType): Column<T> {
    return if (type.isSubtypeOf(this.type)) {
        this as Column<T>
    } else {
        CastColumn(this, type)
    }
}

private class CastColumn<T : Any>(private val origin: Column<*>, override val type: KType) : Column<T> {
    override val name: String get() = origin.name
    override val meta: Meta get() = origin.meta
    override val size: Int get() = origin.size

    @Suppress("UNCHECKED_CAST")
    override fun get(index: Int): T? = origin[index]?.let {
        it as T
    }
}

public class ColumnProperty<C: Any, T : C>(public val table: Table<C>, public val type: KType) : ReadOnlyProperty<Any?, Column<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Column<T> {
        val name = property.name
        return (table.columns[name] ?: error("Column with name $name not found in the table")).cast(type)
    }
}

public operator fun <C: Any, T : C> Collection<Column<C>>.get(header: ColumnHeader<T>): Column<T>? =
    find { it.name == header.name }?.cast(header.type)
