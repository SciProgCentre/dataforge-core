package space.kscience.dataforge.meta

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Check if value is null
 */
public fun Value?.isNull(): Boolean = this == null || this == Null

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

/**
 * Returns this value as an [Instant].
 *
 * A null value is returned as `null`. Numeric values are interpreted as epoch
 * milliseconds. String values are parsed as an instant using [Instant.parse].
 *
 * @throws IllegalStateException if this value is a boolean or list.
 * @throws IllegalArgumentException if this value is a string that cannot be parsed as an instant.
 */
public val Value.instant: Instant?
    get() = when (type) {
        ValueType.NULL -> null
        ValueType.NUMBER -> Instant.fromEpochMilliseconds(number.toLong())
        ValueType.STRING -> Instant.parse(string)
        ValueType.BOOLEAN -> error("Instant cannot be represented as a boolean")
        ValueType.LIST -> error("Instant cannot be represented as a list")
    }

/**
 * Represent [Instant] as value in string representation.
 */
public fun Instant.asValue(): Value = toString().asValue()

/**
 * Returns this value as a duration.
 *
 * A null value is returned as `null`. Numeric values are interpreted as a
 * duration in seconds. String values are parsed as a `Duration`.
 *
 * @throws IllegalStateException if this value is a boolean or list, because
 * it cannot be represented as a duration.
 * @throws IllegalArgumentException if this value is a string that cannot be
 * parsed as a `Duration`.
 */
public val Value.duration: Duration?
    get() = when (type) {
        ValueType.NULL -> null
        ValueType.NUMBER -> double.seconds
        ValueType.STRING -> Duration.parse(string)
        ValueType.BOOLEAN -> error("Duration cannot be represented as a boolean")
        ValueType.LIST -> error("Duration cannot be represented as a list")
    }

/**
 * Represent [Duration] as value in string representation.
 */
public fun Duration.asValue(): Value = toString().asValue()

public fun Value.toMeta(): Meta = Meta(this)