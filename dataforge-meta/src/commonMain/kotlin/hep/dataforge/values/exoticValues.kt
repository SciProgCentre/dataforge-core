package hep.dataforge.values


/**
 * A value built from string which content and type are parsed on-demand
 */
class LazyParsedValue(override val string: String) : Value {
    private val parsedValue by lazy { string.parseValue() }

    override val value: Any? get() = parsedValue.value
    override val type: ValueType get() = parsedValue.type
    override val number: Number get() = parsedValue.number

    override fun toString(): String = string

    override fun equals(other: Any?): Boolean = other is Value && this.parsedValue == other

    override fun hashCode(): Int  = string.hashCode()
}

fun String.lazyParseValue(): LazyParsedValue = LazyParsedValue(this)

/**
 * A performance optimized version of list value for doubles
 */
class DoubleArrayValue(override val value: DoubleArray) : Value {
    override val type: ValueType get() = ValueType.NUMBER
    override val number: Double get() = value.first()
    override val string: String get() = value.first().toString()
    override val list: List<Value> get() = value.map { NumberValue(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false

        return when (other) {
            is DoubleArrayValue -> value.contentEquals(other.value)
            else -> list == other.list
        }
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun toString(): String = list.joinToString (prefix = "[", postfix = "]")
}

fun DoubleArray.asValue(): Value = if(isEmpty()) Null else DoubleArrayValue(this)
