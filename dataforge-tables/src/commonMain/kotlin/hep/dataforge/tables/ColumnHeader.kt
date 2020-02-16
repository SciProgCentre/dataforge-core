package hep.dataforge.tables

import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.meta.string
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import kotlin.reflect.KClass

typealias TableHeader<C> = List<ColumnHeader<C>>

typealias ValueTableHeader = List<ColumnHeader<Value>>

interface ColumnHeader<out T : Any> {
    val name: String
    val type: KClass<out T>
    val meta: Meta
}

data class SimpleColumnHeader<T : Any>(
    override val name: String,
    override val type: KClass<out T>,
    override val meta: Meta
) : ColumnHeader<T>

val ColumnHeader<Value>.valueType: ValueType? get() = meta["valueType"].string?.let { ValueType.valueOf(it) }

val ColumnHeader<Value>.textWidth: Int
    get() = meta["columnWidth"].int ?: when (valueType) {
        ValueType.NUMBER -> 8
        ValueType.STRING -> 16
        ValueType.BOOLEAN -> 5
        ValueType.NULL -> 5
        null -> 16
    }
