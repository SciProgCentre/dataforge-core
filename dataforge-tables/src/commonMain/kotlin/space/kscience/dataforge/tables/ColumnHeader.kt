package space.kscience.dataforge.tables

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType
import kotlin.reflect.KType

public typealias TableHeader<C> = List<ColumnHeader<C>>

public typealias ValueTableHeader = List<ColumnHeader<Value>>

public interface ColumnHeader<out T : Any> {
    public val name: String
    public val type: KType
    public val meta: Meta
}

public data class SimpleColumnHeader<T : Any>(
    override val name: String,
    override val type: KType,
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
