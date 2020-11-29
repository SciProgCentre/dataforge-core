package hep.dataforge.values

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder

/**
 * Check if value is null
 */
public fun Value.isNull(): Boolean = this == Null

/**
 * Check if value is list. This method checks the type of the value, not the number of the elements.
 * So it will return `true` for empty lists and lists of one elements.
 */
public fun Value.isList(): Boolean = this is Iterable<*>

public val Value.boolean: Boolean
    get() = this == True
            || this.list.firstOrNull() == True
            || (type == ValueType.STRING && string.toBoolean())


public val Value.int: Int get() = number.toInt()
public val Value.double: Double get() = number.toDouble()
public val Value.float: Float get() = number.toFloat()
public val Value.short: Short get() = number.toShort()
public val Value.long: Long get() = number.toLong()

public val Value.stringList: List<String> get() = list.map { it.string }

public val Value.doubleArray: DoubleArray
    get() = if (this is DoubleArrayValue) {
        value
    } else {
        DoubleArray(list.size) { list[it].double }
    }


public fun Value.toMeta(): MetaBuilder = Meta { Meta.VALUE_KEY put this }