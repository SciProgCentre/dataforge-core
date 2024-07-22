package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty


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

/**
 * Read this string as lazily parsed value
 */
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

/**
 * A zero-copy wrapping of this [DoubleArray] in a [Value]
 */
public fun DoubleArray.asValue(): Value = if (isEmpty()) Null else DoubleArrayValue(this)

public val Value.doubleArray: DoubleArray
    get() = if (this is DoubleArrayValue) {
        value
    } else {
        DoubleArray(list.size) { list[it].double }
    }

public val Meta?.doubleArray: DoubleArray? get() = this?.value?.doubleArray

public fun MetaProvider.doubleArray(
    vararg default: Double,
    key: Name? = null,
): ReadOnlyProperty<Any?, DoubleArray> = value(
    key,
    reader = { it?.doubleArray ?: doubleArrayOf(*default) },
)

public fun MutableMetaProvider.doubleArray(
    vararg default: Double,
    key: Name? = null,
): ReadWriteProperty<Any?, DoubleArray> = value(
    key,
    writer = { DoubleArrayValue(it) },
    reader = { it?.doubleArray ?: doubleArrayOf(*default) },
)

/**
 * A [Value] wrapping a [ByteArray]
 */
public class ByteArrayValue(override val value: ByteArray) : Value, Iterable<Byte> {
    override val type: ValueType get() = ValueType.LIST
    override val list: List<Value> get() = value.map { NumberValue(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false

        return when (other) {
            is ByteArrayValue -> value.contentEquals(other.value)
            else -> list == other.list
        }
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = list.joinToString(prefix = "[", postfix = "]")

    override fun iterator(): Iterator<Byte> = value.iterator()
}

public fun ByteArray.asValue(): Value = ByteArrayValue(this)

public val Value.byteArray: ByteArray
    get() = if (this is ByteArrayValue) {
        value
    } else {
        ByteArray(list.size) { list[it].number.toByte() }
    }

public val Meta?.byteArray: ByteArray? get() = this?.value?.byteArray

public fun MetaProvider.byteArray(
    vararg default: Byte,
    key: Name? = null,
): ReadOnlyProperty<Any?, ByteArray> = value(
    key,
    reader = { it?.byteArray ?: byteArrayOf(*default) },
)

public fun MutableMetaProvider.byteArray(
    vararg default: Byte,
    key: Name? = null,
): ReadWriteProperty<Any?, ByteArray> = value(
    key,
    writer = { ByteArrayValue(it) },
    reader = { it?.byteArray ?: byteArrayOf(*default) },
)