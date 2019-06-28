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
}

fun String.lazyParseValue(): LazyParsedValue  = LazyParsedValue(this)

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

        return if (other is DoubleArrayValue) {
            value.contentEquals(other.value)
        } else {
            list == other.list
        }
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun toString(): String = value.toString()
}

fun DoubleArray.asValue(): DoubleArrayValue = DoubleArrayValue(this)
