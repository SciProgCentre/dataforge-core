package hep.dataforge.values

import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta

/**
 * Check if value is null
 */
fun Value.isNull(): Boolean = this == Null

/**
 * Check if value is list
 */
fun Value.isList(): Boolean = this.list.size > 1

val Value.boolean
    get() = this == True
            || this.list.firstOrNull() == True
            || (type == ValueType.STRING && string.toBoolean())


val Value.int get() = number.toInt()
val Value.double get() = number.toDouble()
val Value.float get() = number.toFloat()
val Value.short get() = number.toShort()
val Value.long get() = number.toLong()

val Value.stringList: List<String> get() = list.map { it.string }

val Value.doubleArray: DoubleArray
    get() = if (this is DoubleArrayValue) {
        value
    } else {
        DoubleArray(list.size) { list[it].double }
    }


fun Value.toMeta() = buildMeta { Meta.VALUE_KEY put this }