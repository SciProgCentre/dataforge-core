package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.safeCast

@Suppress("UNCHECKED_CAST")
public fun <T : Any> Column<*>.cast(type: KClass<out T>): Column<T> {
    return if (type.isSubclassOf(this.type)) {
        this as Column<T>
    } else {
        CastColumn(this, type)
    }
}

public class CastColumn<T : Any>(private val origin: Column<*>, override val type: KClass<out T>) : Column<T> {
    override val name: String get() = origin.name
    override val meta: Meta get() = origin.meta
    override val size: Int get() = origin.size


    override fun get(index: Int): T? = type.safeCast(origin[index])
}

public class ColumnProperty<C: Any, T : C>(public val table: Table<C>, public val type: KClass<T>) : ReadOnlyProperty<Any?, Column<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Column<T> {
        val name = property.name
        return (table.columns[name] ?: error("Column with name $name not found in the table")).cast(type)
    }
}

public operator fun <C: Any, T : C> Collection<Column<C>>.get(header: ColumnHeader<T>): Column<T>? =
    find { it.name == header.name }?.cast(header.type)
