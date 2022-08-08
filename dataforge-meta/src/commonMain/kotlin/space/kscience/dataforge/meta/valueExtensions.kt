package space.kscience.dataforge.meta

/**
 * Check if value is null
 */
public fun Value.isNull(): Boolean = this == Null

/**
 * Check if value is list.
 */
public fun Value.isList(): Boolean = this.type == ValueType.LIST

public val Value.boolean: Boolean
    get() = this == True
            || this.list.firstOrNull() == True
            || (type == ValueType.STRING && string.toBoolean())


public val Value.int: Int get() = number.toInt()
public val Value.double: Double get() = number.toDouble()
public val Value.float: Float get() = number.toFloat()
public val Value.short: Short get() = number.toShort()
public val Value.long: Long get() = number.toLong()

public inline fun <reified E : Enum<E>> Value.enum(): E = if (this is EnumValue<*>) {
    value as E
} else {
    enumValueOf<E>(string)
}


public val Value.stringList: List<String> get() = list.map { it.string }

public val Value.doubleArray: DoubleArray
    get() = if (this is DoubleArrayValue) {
        value
    } else {
        DoubleArray(list.size) { list[it].double }
    }


public fun Value.toMeta(): Meta = Meta(this)