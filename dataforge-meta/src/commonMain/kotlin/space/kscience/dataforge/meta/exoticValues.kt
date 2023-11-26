package space.kscience.dataforge.meta


/**
 * A value built from string which content and type are parsed on-demand
 */
public class LazyParsedValue(public val string: String) : Value {
    private val parsedValue by lazy { Value.parse(string) }

    override val value: Any? get() = parsedValue.value
    override val type: ValueType get() = parsedValue.type

    override fun toString(): String = string

    override fun equals(other: Any?): Boolean = other is Value && this.parsedValue == other

    override fun hashCode(): Int = string.hashCode()
}

public fun String.lazyParseValue(): LazyParsedValue = LazyParsedValue(this)

/**
 * A performance optimized version of list value for doubles
 */
public class DoubleArrayValue(override val value: DoubleArray) : Value, Iterable<Double> {
    override val type: ValueType get() = ValueType.LIST
    override val list: List<Value> get() = value.map { NumberValue(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false

        return when (other) {
            is DoubleArrayValue -> value.contentEquals(other.value)
            else -> list == other.list
        }
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = list.joinToString(prefix = "[", postfix = "]")

    override fun iterator(): Iterator<Double> = value.iterator()
}

public fun DoubleArray.asValue(): Value = if (isEmpty()) Null else DoubleArrayValue(this)
