package hep.dataforge.values

import kotlinx.serialization.Serializable


/**
 * The list of supported Value types.
 *
 * Time value and binary value are represented by string
 *
 */
@Serializable
public enum class ValueType {
    NUMBER, STRING, BOOLEAN, NULL
}

/**
 * A wrapper class for both Number and non-Number objects.
 *
 * Value can represent a list of value objects.
 */
public interface Value {
    /**
     * Get raw value of this value
     */
    public val value: Any?

    /**
     * The type of the value
     */
    public val type: ValueType

    /**
     * get this value represented as Number
     */
    public val number: Number

    /**
     * get this value represented as String
     */
    public val string: String

    /**
     * get this value represented as List
     */
    public val list: List<Value> get() = if (this == Null) emptyList() else listOf(this)

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    public companion object {
        public const val TARGET: String = "value"

        /**
         * Convert object to value
         */
        public fun of(value: Any?): Value {
            return when (value) {
                null -> Null
                is Value -> value
                true -> True
                false -> False
                is Number -> value.asValue()
                is Iterable<*> -> {
                    val list = value.map { of(it) }
                    if (list.isEmpty()) {
                        Null
                    } else {
                        ListValue(list)
                    }
                }
                is DoubleArray -> value.asValue()
                is IntArray -> value.asValue()
                is FloatArray -> value.asValue()
                is ShortArray -> value.asValue()
                is LongArray -> value.asValue()
                is ByteArray -> value.asValue()
                is Array<*> -> ListValue(value.map { of(it) })
                is Enum<*> -> EnumValue(value)
                is CharSequence -> StringValue(value.toString())
                else -> throw IllegalArgumentException("Unrecognized type of the object (${value::class}) converted to Value")
            }
        }
    }
}

/**
 * A singleton null value
 */
public object Null : Value {
    override val value: Any? get() = null
    override val type: ValueType get() = ValueType.NULL
    override val number: Number get() = Double.NaN
    override val string: String get() = "@null"

    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other === Null
    override fun hashCode(): Int = 0
}

/**
 * Singleton true value
 */
public object True : Value {
    override val value: Any? get() = true
    override val type: ValueType get() = ValueType.BOOLEAN
    override val number: Number get() = 1.0
    override val string: String get() = "true"

    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other === True
    override fun hashCode(): Int = 1
}

/**
 * Singleton false value
 */
public object False : Value {
    override val value: Any? get() = false
    override val type: ValueType get() = ValueType.BOOLEAN
    override val number: Number get() = -1.0
    override val string: String get() = "false"

    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other === False
    override fun hashCode(): Int = -1
}

public class NumberValue(override val number: Number) : Value {
    override val value: Any? get() = number
    override val type: ValueType get() = ValueType.NUMBER
    override val string: String get() = number.toString()

    override fun equals(other: Any?): Boolean {
        if (other !is Value) return false
        return when (number) {
            is Short -> number.toShort() == other.number.toShort()
            is Long -> number.toLong() == other.number.toLong()
            is Byte -> number.toByte() == other.number.toByte()
            is Int -> number.toInt() == other.number.toInt()
            is Float -> number.toFloat() == other.number.toFloat()
            is Double -> number.toDouble() == other.number.toDouble()
            else -> number.toString() == other.number.toString()
        }
    }

    override fun hashCode(): Int = number.hashCode()

    override fun toString(): String = value.toString()
}

public class StringValue(override val string: String) : Value {
    override val value: Any? get() = string
    override val type: ValueType get() = ValueType.STRING
    override val number: Number get() = string.toDouble()

    override fun equals(other: Any?): Boolean {
        return this.string == (other as? Value)?.string
    }

    override fun hashCode(): Int = string.hashCode()

    override fun toString(): String = "\"${value.toString()}\""
}

public class EnumValue<E : Enum<*>>(override val value: E) : Value {
    override val type: ValueType get() = ValueType.STRING
    override val number: Number get() = value.ordinal
    override val string: String get() = value.name

    override fun equals(other: Any?): Boolean {
        return string == (other as? Value)?.string
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}

public class ListValue(override val list: List<Value>) : Value {
    init {
        require(list.isNotEmpty()) { "Can't create list value from empty list" }
    }

    override val value: List<Value> get() = list
    override val type: ValueType get() = list.first().type
    override val number: Number get() = list.first().number
    override val string: String get() = list.first().string

    override fun toString(): String = list.joinToString(prefix = "[", postfix = "]")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false
        if (other is DoubleArrayValue) {
            return DoubleArray(list.size) { list[it].number.toDouble() }.contentEquals(other.value)
        }
        return list == other.list
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }


}

public fun Number.asValue(): Value = NumberValue(this)

public fun Boolean.asValue(): Value = if (this) True else False

public fun String.asValue(): Value = StringValue(this)

public fun Iterable<Value>.asValue(): Value {
    val list = toList()
    return if (list.isEmpty()) Null else ListValue(this.toList())
}

public fun IntArray.asValue(): Value = if (isEmpty()) Null else ListValue(map { NumberValue(it) })

public fun LongArray.asValue(): Value = if (isEmpty()) Null else ListValue(map { NumberValue(it) })

public fun ShortArray.asValue(): Value = if (isEmpty()) Null else ListValue(map { NumberValue(it) })

public fun FloatArray.asValue(): Value = if (isEmpty()) Null else ListValue(map { NumberValue(it) })

public fun ByteArray.asValue(): Value = if (isEmpty()) Null else ListValue(map { NumberValue(it) })

public fun <E : Enum<E>> E.asValue(): Value = EnumValue(this)


/**
 * Create Value from String using closest match conversion
 */
public fun String.parseValue(): Value {

    //Trying to get integer
    if (isEmpty() || this == Null.string) {
        return Null
    }

    //string constants
    if (startsWith("\"") && endsWith("\"")) {
        return StringValue(substring(1, length - 2))
    }

    toIntOrNull()?.let {
        return NumberValue(it)
    }

    toDoubleOrNull()?.let {
        return NumberValue(it)
    }

    if ("true" == this) {
        return True
    }

    if ("false" == this) {
        return False
    }

    //Give up and return a StringValue
    return StringValue(this)
}