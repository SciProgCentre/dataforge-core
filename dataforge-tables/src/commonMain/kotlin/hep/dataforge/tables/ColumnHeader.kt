package hep.dataforge.tables

import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.meta.string
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import kotlin.reflect.KClass

public typealias TableHeader<C> = List<ColumnHeader<C>>

public typealias ValueTableHeader = List<ColumnHeader<Value>>

public interface ColumnHeader<out T : Any> {
    public val name: String
    public val type: KClass<out T>
    public val meta: Meta
}

public data class SimpleColumnHeader<T : Any>(
    override val name: String,
    override val type: KClass<out T>,
    override val meta: Meta
) : ColumnHeader<T>

public val ColumnHeader<Value>.valueType: ValueType? get() = meta["valueType"].string?.let { ValueType.valueOf(it) }

public val ColumnHeader<Value>.textWidth: Int
    get() = meta["columnWidth"].int ?: when (valueType) {
        ValueType.NUMBER -> 8
        ValueType.STRING -> 16
        ValueType.BOOLEAN -> 5
        ValueType.NULL -> 5
        ValueType.LIST -> 32
        null -> 16
    }
