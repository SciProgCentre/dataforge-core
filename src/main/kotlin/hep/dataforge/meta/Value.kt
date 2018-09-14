package hep.dataforge.meta


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
        /**
         * Convert object to value
         */
        fun of(value: Any?): Value {
            return when (value) {
                null -> Null
                true -> True
                false -> False
                is Number -> NumberValue(value)
                is Iterable<*> -> ListValue(value.map { of(it) })
                is Enum<*> -> EnumValue(value)
                is CharSequence -> StringValue(value.toString())
                else -> throw IllegalArgumentException("Unrecognized type of the object converted to Value")
            }
        }
    }
}

/**
 * A singleton null value
 */
object Null : Value {
    override val value: Any? = null
    override val type: ValueType = ValueType.NULL
    override val number: Number = Double.NaN
    override val string: String = "@null"
}

/**
 * Check if value is null
 */
fun Value.isNull(): Boolean = this == Null


/**
 * Singleton true value
 */
object True : Value {
    override val value: Any? = true
    override val type: ValueType = ValueType.BOOLEAN
    override val number: Number = 1.0
    override val string: String = "+"
}

/**
 * Singleton false value
 */
object False : Value {
    override val value: Any? = false
    override val type: ValueType = ValueType.BOOLEAN
    override val number: Number = -1.0
    override val string: String = "-"
}

val Value.boolean get() = this == True || this.list.firstOrNull() == True || (type == ValueType.STRING && string.toBoolean())

class NumberValue(override val number: Number) : Value {
    override val value: Any? get() = number
    override val type: ValueType = ValueType.NUMBER
    override val string: String get() = number.toString()
}

class StringValue(override val string: String) : Value {
    override val value: Any? get() = string
    override val type: ValueType = ValueType.STRING
    override val number: Number get() = string.toDouble()
}

class EnumValue<E : Enum<*>>(override val value: E) : Value {
    override val type: ValueType = ValueType.STRING
    override val number: Number = value.ordinal
    override val string: String = value.name
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