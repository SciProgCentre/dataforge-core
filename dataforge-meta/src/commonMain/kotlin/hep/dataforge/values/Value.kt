package hep.dataforge.values


/**
 * The list of supported Value types.
 *
 * Time value and binary value are represented by string
 *
 */
enum class ValueType {
    NUMBER, STRING, BOOLEAN, NULL
}

/**
 * A wrapper class for both Number and non-Number objects.
 *
 * Value can represent a list of value objects.
 */
interface Value {
    /**
     * Get raw value of this value
     */
    val value: Any?

    /**
     * The type of the value
     */
    val type: ValueType

    /**
     * get this value represented as Number
     */
    val number: Number

    /**
     * get this value represented as String
     */
    val string: String

    /**
     * get this value represented as List
     */
    val list: List<Value>
        get() = listOf(this)

    companion object {
        const val TYPE = "value"

        /**
         * Convert object to value
         */
        fun of(value: Any?): Value {
            return when (value) {
                null -> Null
                is Value -> value
                true -> True
                false -> False
                is Number -> NumberValue(value)
                is Iterable<*> -> ListValue(value.map { of(it) })
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
object Null : Value {
    override val value: Any? get() = null
    override val type: ValueType get() = ValueType.NULL
    override val number: Number get() = Double.NaN
    override val string: String get() = "@null"

    override fun toString(): String = value.toString()
}

/**
 * Check if value is null
 */
fun Value.isNull(): Boolean = this == Null


/**
 * Singleton true value
 */
object True : Value {
    override val value: Any? get() = true
    override val type: ValueType get() = ValueType.BOOLEAN
    override val number: Number get() = 1.0
    override val string: String get() = "+"

    override fun toString(): String = value.toString()
}

/**
 * Singleton false value
 */
object False : Value {
    override val value: Any? get() = false
    override val type: ValueType get() = ValueType.BOOLEAN
    override val number: Number get() = -1.0
    override val string: String get() = "-"
}

val Value.boolean get() = this == True || this.list.firstOrNull() == True || (type == ValueType.STRING && string.toBoolean())

class NumberValue(override val number: Number) : Value {
    override val value: Any? get() = number
    override val type: ValueType get() = ValueType.NUMBER
    override val string: String get() = number.toString()

    override fun equals(other: Any?): Boolean {
        if (other !is Value) return false
        return when (number) {
            is Short -> number == other.number.toShort()
            is Long -> number == other.number.toLong()
            is Byte -> number == other.number.toByte()
            is Int -> number == other.number.toInt()
            is Float -> number == other.number.toFloat()
            is Double -> number == other.number.toDouble()
            else -> number.toString() == other.number.toString()
        }
    }

    override fun hashCode(): Int = number.hashCode()

    override fun toString(): String = value.toString()
}

class StringValue(override val string: String) : Value {
    override val value: Any? get() = string
    override val type: ValueType get() = ValueType.STRING
    override val number: Number get() = string.toDouble()

    override fun equals(other: Any?): Boolean {
        return this.string == (other as? Value)?.string
    }

    override fun hashCode(): Int = string.hashCode()

    override fun toString(): String = value.toString()
}

class EnumValue<E : Enum<*>>(override val value: E) : Value {
    override val type: ValueType get() = ValueType.STRING
    override val number: Number get() = value.ordinal
    override val string: String get() = value.name

    override fun equals(other: Any?): Boolean {
        return string == (other as? Value)?.string
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}

class ListValue(override val list: List<Value>) : Value {
    init {
        if (list.isEmpty()) {
            throw IllegalArgumentException("Can't create list value from empty list")
        }
    }

    override val value: Any? get() = list
    override val type: ValueType get() = list.first().type
    override val number: Number get() = list.first().number
    override val string: String get() = list.first().string

    override fun toString(): String = value.toString()
}

/**
 * Check if value is list
 */
fun Value.isList(): Boolean = this.list.size > 1


fun Number.asValue(): Value = NumberValue(this)

fun Boolean.asValue(): Value = if (this) True else False

fun String.asValue(): Value = StringValue(this)

fun Collection<Value>.asValue(): Value = ListValue(this.toList())


/**
 * Create Value from String using closest match conversion
 */
fun String.parseValue(): Value {

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

class LazyParsedValue(override val string: String) : Value {
    private val parsedValue by lazy { string.parseValue() }

    override val value: Any?
        get() = parsedValue.value
    override val type: ValueType
        get() = parsedValue.type
    override val number: Number
        get() = parsedValue.number

    override fun toString(): String = value.toString()
}