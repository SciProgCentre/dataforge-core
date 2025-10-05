package space.kscience.dataforge.meta

/**
 * Check if value is null
 */
public fun Value.isNull(): Boolean = this == Null

/**
 * Check if the value is a list.
 */
public fun Value.isList(): Boolean = this.type == ValueType.LIST

public val Value.boolean: Boolean
    get() = when (type) {
        ValueType.NUMBER -> int > 0
        ValueType.STRING -> string.toBoolean()
        ValueType.BOOLEAN -> this === True
        ValueType.LIST -> list.singleOrNull()?.boolean == true
        ValueType.NULL -> false
    }

//        this == True
//            || this.list.firstOrNull() == True
//            || (type == ValueType.STRING && string.toBoolean())
//            || (type == ValueType.)

/**
 * Strictly converts this [Value] to a [UInt], returning `null` if the conversion is lossy or impossible.
 * - For numeric values, returns a `UInt` only if the number is a non-negative integer within the `UInt` range.
 * - For string values, uses `String.toUIntOrNull()`.
 * - This accessor is **strict** and avoids silent truncation or reinterpretation of out-of-range numbers.
 */
public val Value.uint: UInt?
    get() = when (this) {
        is NumberValue -> number.toLong().takeIf { it in 0L..UInt.MAX_VALUE.toLong() }?.toUInt()
        else -> string.toUIntOrNull()
    }

/**
 * Converts this [Value] to a [UInt] using Kotlin's standard coercing (truncating) conversion.
 * Use with caution, as this can lead to data loss if the original number is outside the `UInt` range.
 * For example, `(UInt.MAX_VALUE.toLong() + 1L)` will be truncated.
 */
public val Value.uintCoerced: UInt? get() = numberOrNull?.toLong()?.toUInt()

/**
 * Strictly converts this [Value] to a [ULong], returning `null` if the conversion is lossy or impossible.
 * - For numeric values, returns a `ULong` only if the number is a non-negative integer.
 * - For string values, uses `String.toULongOrNull()`.
 * - This accessor is **strict** and avoids silent bitwise reinterpretation of negative numbers.
 */
public val Value.ulong: ULong?
    get() = when (this) {
        is NumberValue -> number.toLong().takeIf { it >= 0L }?.toULong()
        else -> string.toULongOrNull()
    }

/**
 * Converts this [Value] to a [ULong] using Kotlin's standard coercing (bitwise reinterpreting) conversion.
 * This will reinterpret negative `Long` values - for example, `-1L` becomes `ULong.MAX_VALUE`.
 */
public val Value.ulongCoerced: ULong? get() = numberOrNull?.toLong()?.toULong()

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


public fun Value.toMeta(): Meta = Meta(this)