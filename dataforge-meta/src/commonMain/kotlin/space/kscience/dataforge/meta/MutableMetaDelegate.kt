package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.getIndexedList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


/* Read-write delegates */

public interface MutableMetaDelegate<T> : ReadWriteProperty<Any?, T>, Described

public fun MutableMetaProvider.node(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MutableMetaDelegate<Meta?> = object : MutableMetaDelegate<Meta?> {

    override val descriptor: MetaDescriptor? = descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): Meta? {
        return get(key ?: Name.of(property.name))
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Meta?) {
        val name = key ?: Name.of(property.name)
        set(name, value)
    }
}

/**
 * Use [converter] to transform an object to Meta and back.
 * Note that mutation of the object does not change Meta.
 */
public fun <T> MutableMetaProvider.convertable(
    converter: MetaConverter<T>,
    key: Name? = null,
): MutableMetaDelegate<T?> = object : MutableMetaDelegate<T?> {

    override val descriptor: MetaDescriptor? get() = converter.descriptor


    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: Name.of(property.name)
        return get(name)?.let { converter.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: Name.of(property.name)
        set(name, value?.let { converter.convert(it) })
    }
}

public fun <T> MutableMetaProvider.convertable(
    converter: MetaConverter<T>,
    default: T,
    key: Name? = null,
): MutableMetaDelegate<T> = object : MutableMetaDelegate<T> {

    override val descriptor: MetaDescriptor? get() = converter.descriptor


    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: Name.of(property.name)
        return get(name)?.let { converter.read(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: Name.of(property.name)
        set(name, value?.let { converter.convert(it) })
    }
}

/**
 * Use object serializer to transform it to Meta and back.
 * Note that mutation of the object does not change Meta.
 */
@DFExperimental
public inline fun <reified T> MutableMetaProvider.serializable(
    descriptor: MetaDescriptor? = null,
    key: Name? = null,
): MutableMetaDelegate<T?> = convertable<T>(MetaConverter.serializable(descriptor), key)

@DFExperimental
public inline fun <reified T> MutableMetaProvider.serializable(
    descriptor: MetaDescriptor? = null,
    default: T,
    key: Name? = null,
): MutableMetaDelegate<T> = convertable(MetaConverter.serializable(descriptor), default, key)

/**
 * Use [converter] to convert a list of same name siblings meta to object and back.
 * Note that mutation of the object does not change Meta.
 */
public fun <T> MutableMeta.listOfConvertable(
    converter: MetaConverter<T>,
    key: Name? = null,
): MutableMetaDelegate<List<T>> = object : MutableMetaDelegate<List<T>> {
    override val descriptor: MetaDescriptor? = converter.descriptor?.copy(multiple = true)

    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: Name.of(property.name)
        return getIndexedList(name).map { converter.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        val name = key ?: Name.of(property.name)
        setIndexed(name, value.map { converter.convert(it) })
    }
}

@DFExperimental
public inline fun <reified T> MutableMeta.listOfSerializable(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MutableMetaDelegate<List<T>> = listOfConvertable(MetaConverter.serializable(descriptor), key)


public fun MutableMetaProvider.value(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MutableMetaDelegate<Value?> = object : MutableMetaDelegate<Value?> {
    override val descriptor: MetaDescriptor? = descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): Value? =
        get(key ?: Name.of(property.name))?.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Value?) {
        setValue(key ?: Name.of(property.name), value)
    }
}

public fun <T> MutableMetaProvider.value(
    key: Name? = null,
    writer: (T) -> Value? = { Value.of(it) },
    descriptor: MetaDescriptor? = null,
    reader: (Value?) -> T,
): MutableMetaDelegate<T> = object : MutableMetaDelegate<T> {
    override val descriptor: MetaDescriptor? = descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        reader(get(key ?: Name.of(property.name))?.value)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setValue(key ?: Name.of(property.name), writer(value))
    }
}

/* Read-write delegates for [MutableItemProvider] */

public fun MutableMetaProvider.string(key: Name? = null): MutableMetaDelegate<String?> =
    value(key) { it?.string }

public fun MutableMetaProvider.boolean(key: Name? = null): MutableMetaDelegate<Boolean?> =
    value(key) { it?.boolean }

public fun MutableMetaProvider.number(key: Name? = null): MutableMetaDelegate<Number?> =
    value(key) { it?.number }

public fun MutableMetaProvider.string(default: String, key: Name? = null): MutableMetaDelegate<String> =
    value(key) { it?.string ?: default }

public fun MutableMetaProvider.boolean(default: Boolean, key: Name? = null): MutableMetaDelegate<Boolean> =
    value(key) { it?.boolean ?: default }

public fun MutableMetaProvider.number(default: Number, key: Name? = null): MutableMetaDelegate<Number> =
    value(key) { it?.number ?: default }

public fun MutableMetaProvider.string(key: Name? = null, default: () -> String): MutableMetaDelegate<String> =
    value(key) { it?.string ?: default() }

public fun MutableMetaProvider.boolean(key: Name? = null, default: () -> Boolean): MutableMetaDelegate<Boolean> =
    value(key) { it?.boolean ?: default() }

public fun MutableMetaProvider.number(key: Name? = null, default: () -> Number): MutableMetaDelegate<Number> =
    value(key) { it?.number ?: default() }

public inline fun <reified E : Enum<E>> MutableMetaProvider.enum(
    default: E,
    key: Name? = null,
): MutableMetaDelegate<E> = value(key) { value ->
    value?.string?.let { str -> enumValues<E>().firstOrNull { it.name == str } } ?: default
}

/* Number delegates */

public fun MutableMetaProvider.int(key: Name? = null): MutableMetaDelegate<Int?> =
    value(key) { it?.int }

public fun MutableMetaProvider.double(key: Name? = null): MutableMetaDelegate<Double?> =
    value(key) { it?.double }

public fun MutableMetaProvider.long(key: Name? = null): MutableMetaDelegate<Long?> =
    value(key) { it?.long }

public fun MutableMetaProvider.float(key: Name? = null): MutableMetaDelegate<Float?> =
    value(key) { it?.float }


/* Safe number delegates*/

public fun MutableMetaProvider.int(default: Int, key: Name? = null): MutableMetaDelegate<Int> =
    value(key) { it?.int ?: default }

public fun MutableMetaProvider.double(default: Double, key: Name? = null): MutableMetaDelegate<Double> =
    value(key) { it?.double ?: default }

public fun MutableMetaProvider.long(default: Long, key: Name? = null): MutableMetaDelegate<Long> =
    value(key) { it?.long ?: default }

public fun MutableMetaProvider.float(default: Float, key: Name? = null): MutableMetaDelegate<Float> =
    value(key) { it?.float ?: default }


/* Extra delegates for special cases */

public fun MutableMetaProvider.stringList(
    vararg default: String,
    key: Name? = null,
): MutableMetaDelegate<List<String>> = value(
    key,
    writer = { list -> list.map { str -> str.asValue() }.asValue() },
    reader = { it?.stringList ?: listOf(*default) },
)

public fun MutableMetaProvider.stringList(
    key: Name? = null,
): MutableMetaDelegate<List<String>?> = value(
    key,
    writer = { it -> it?.map { str -> str.asValue() }?.asValue() },
    reader = { it?.stringList },
)

public fun MutableMetaProvider.numberList(
    vararg default: Number,
    key: Name? = null,
): MutableMetaDelegate<List<Number>> = value(
    key,
    writer = { it.map { num -> num.asValue() }.asValue() },
    reader = { it?.list?.map { value -> value.numberOrNull ?: Double.NaN } ?: listOf(*default) },
)


public fun <T> MutableMetaProvider.listValue(
    key: Name? = null,
    writer: (T) -> Value = { Value.of(it) },
    reader: (Value) -> T,
): MutableMetaDelegate<List<T>?> = value(
    key,
    writer = { it?.map(writer)?.asValue() },
    reader = { it?.list?.map(reader) }
)

/**
 * Creates a mutable metadata delegate for an [Instant] value.
 *
 * The value is read from the metadata entry identified by [key], or by the delegated property name when [key] is null.
 * Missing or null metadata values are replaced with the value produced by [default].
 * Numeric values are interpreted as epoch milliseconds, and string values are parsed as [Instant].
 * Boolean and list values are not supported and cause an error when read.
 * Written values are stored as string values using the [Instant] string representation.
 *
 * @param key optional metadata key to use instead of the delegated property name.
 * @param default provider of the value used when the metadata entry is missing or null.
 * @return a mutable delegate that reads and writes an [Instant] metadata value.
 */
public fun MutableMetaProvider.instant(
    key: Name? = null,
    default: () -> Instant
): MutableMetaDelegate<Instant> = value(
    key = key,
    writer = {
        it.toString().asValue()
    },
    reader = {
        when (it?.type) {
            null, ValueType.NULL -> default()
            ValueType.NUMBER -> Instant.fromEpochMilliseconds(it.number.toLong())
            ValueType.STRING -> Instant.parse(it.string)
            ValueType.BOOLEAN -> error("Instant cannot be represented as a boolean")
            ValueType.LIST -> error("Instant cannot be represented as a list")
        }
    }
)

/**
 * Creates a mutable delegate for a [Duration] value stored in this meta provider.
 *
 * The value is read from the specified [key], or from the delegated property name when [key] is null.
 * Missing or null values are resolved using [default]. Numeric values are interpreted as seconds,
 * and string values are parsed with [Duration.parse]. Boolean and list values are not supported
 * and cause an error when read.
 *
 * Written durations are stored as string values using their standard string representation.
 *
 * @param key The meta value name to bind the delegate to, or null to use the delegated property name.
 * @param default A function that provides the duration when the stored value is missing or null.
 * @return A mutable meta delegate that reads and writes [Duration] values.
 */
public fun MutableMetaProvider.duration(
    key: Name? = null,
    default: () -> Duration
): MutableMetaDelegate<Duration> = value(
    key = key,
    writer = {
        it.toString().asValue()
    },
    reader = {
        when (it?.type) {
            null, ValueType.NULL -> default()
            ValueType.NUMBER -> it.double.seconds
            ValueType.STRING -> Duration.parse(it.string)
            ValueType.BOOLEAN -> error("Duration cannot be represented as a boolean")
            ValueType.LIST -> error("Duration cannot be represented as a list")
        }
    }
)