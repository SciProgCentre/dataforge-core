package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline


/**
 * The list of supported Value types.
 *
 * Time value and binary value are represented by string
 *
 */
@Serializable
public enum class ValueType {
    NUMBER, STRING, BOOLEAN, LIST, NULL
}

/**
 * A wrapper class for both Number and non-Number objects.
 *
 * Value can represent a list of value objects.
 */
@Serializable(ValueSerializer::class)
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
     * get this value represented as List
     */
    public val list: List<Value> get() = if (this == Null) emptyList() else listOf(this)

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String

    public companion object {
        public const val TYPE: String = "value"

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

public val Value.string: String get() = toString()

/**
 * get this value represented as Number
 */
public val Value.numberOrNull: Number?
    get() = if (this is NumberValue) number else string.toDoubleOrNull()

/**
 * Return [Value] number content or throw error if value is not a number
 */
public val Value.number: Number
    get() = (if (this is NumberValue) number else numberOrNull ?: error("The value is not a number"))

/**
 * A singleton null value
 */
public object Null : Value {
    override val value: Any? get() = null
    override val type: ValueType get() = ValueType.NULL
    override fun toString(): String = "@null"

    override fun equals(other: Any?): Boolean = other === Null
    override fun hashCode(): Int = 0
}

/**
 * Singleton true value
 */
public object True : Value {
    override val value: Any get() = true
    override val type: ValueType get() = ValueType.BOOLEAN
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other === True
    override fun hashCode(): Int = 1
}

/**
 * Singleton false value
 */
public object False : Value {
    override val value: Any get() = false
    override val type: ValueType get() = ValueType.BOOLEAN
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other === False
    override fun hashCode(): Int = -1
}

public class NumberValue(public val number: Number) : Value {
    override val value: Any get() = number
    override val type: ValueType get() = ValueType.NUMBER

    override fun toString(): String = number.toString()

    override fun equals(other: Any?): Boolean {
        if (other !is Value) return false

        val otherNumber = other.numberOrNull ?: return false

        if(number == otherNumber) return true

        //Do not change the order of comparison. On JS number is the instance of all types
        return when (numberOrNull) {
            is Double -> number.toDouble() == otherNumber.toDouble()
            is Float -> number.toFloat() == otherNumber.toFloat()
            is Long -> number.toLong() == otherNumber.toLong()
            is Short -> number.toShort() == otherNumber.toShort()
            is Int -> number.toInt() == otherNumber.toInt()
            is Byte -> number.toByte() == otherNumber.toByte()
            else -> false
        }
    }

    override fun hashCode(): Int = numberOrNull.hashCode()
}

@JvmInline
public value class StringValue(public val string: String) : Value {
    override val value: Any get() = string
    override val type: ValueType get() = ValueType.STRING
    override fun toString(): String = string
}

public class EnumValue<E : Enum<*>>(override val value: E) : Value {
    override val type: ValueType get() = ValueType.STRING

    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = string == (other as? Value)?.string

    override fun hashCode(): Int = value.hashCode()
}

public class ListValue(override val list: List<Value>) : Value, Iterable<Value> {
    override val value: List<Value> get() = list
    override val type: ValueType get() = ValueType.LIST

    override fun toString(): String = list.joinToString(prefix = "[", postfix = "]")

    override fun iterator(): Iterator<Value> = list.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false
        if (other is DoubleArrayValue) {
            return DoubleArray(list.size) { list[it].numberOrNull?.toDouble() ?: Double.NaN }.contentEquals(other.value)
        }
        return list == other.list
    }

    override fun hashCode(): Int = list.hashCode()

    public companion object {
        public val EMPTY: ListValue = ListValue(emptyList())
    }
}

public fun ListValue(vararg numbers: Number): ListValue = ListValue(numbers.map { it.asValue() })
public fun ListValue(vararg strings: String): ListValue = ListValue(strings.map { it.asValue() })

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
 * Create Value from String using the closest match conversion
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