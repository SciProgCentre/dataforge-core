package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/* Meta delegates */

public interface MetaDelegate<T> : ReadOnlyProperty<Any?, T>, Described


public fun MetaProvider.node(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<Meta?> = object : MetaDelegate<Meta?> {
    override val descriptor: MetaDescriptor? = descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): Meta? {
        return get(key ?: Name.of(property.name))
    }
}


/**
 * Use [reader] to read the Meta node
 */
public fun <T> MetaProvider.readable(
    reader: MetaReader<T>,
    key: Name? = null,
): MetaDelegate<T?> = object : MetaDelegate<T?> {
    override val descriptor: MetaDescriptor? get() = reader.descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return get(key ?: Name.of(property.name))?.let { reader.read(it) }
    }
}

/**
 * Use [reader] to read the Meta node or return [default] if node does not exist
 */
public fun <T> MetaProvider.readable(
    reader: MetaReader<T>,
    default: T,
    key: Name? = null,
): MetaDelegate<T> = object : MetaDelegate<T> {
    override val descriptor: MetaDescriptor? get() = reader.descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get(key ?: Name.of(property.name))?.let { reader.read(it) } ?: default
    }
}

/**
 * Use [reader] to read the Meta node
 */
@Deprecated("Replace with readable", ReplaceWith("readable(metaReader, key)"))
public fun <T> MetaProvider.spec(
    reader: MetaReader<T>,
    key: Name? = null,
): MetaDelegate<T?> = readable(reader, key)

/**
 * Use object serializer to transform it to Meta and back
 */
@DFExperimental
public inline fun <reified T> MetaProvider.serializable(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<T?> = readable(MetaConverter.serializable(descriptor), key)

@DFExperimental
public inline fun <reified T> MetaProvider.serializable(
    key: Name? = null,
    default: T,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<T> = readable(MetaConverter.serializable(descriptor), default, key)

@Deprecated("Use convertable", ReplaceWith("convertable(converter, key)"))
public fun <T> MetaProvider.node(
    key: Name? = null,
    converter: MetaReader<T>,
): ReadOnlyProperty<Any?, T?> = readable(converter, key)

/**
 * Use [reader] to convert a list of same name siblings meta to object
 */
public fun <T> Meta.listOfReadable(
    reader: MetaReader<T>,
    key: Name? = null,
): MetaDelegate<List<T>> = object : MetaDelegate<List<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: Name.of(property.name)
        return getIndexed(name).values.map { reader.read(it) }
    }

    override val descriptor: MetaDescriptor? = reader.descriptor?.copy(multiple = true)
}


/**
 * Use [converter] to convert a list of same name siblings meta to object
 */
@Deprecated("Replace with readingList", ReplaceWith("readingList(converter, key)"))
public fun <T> Meta.listOfSpec(
    converter: MetaReader<T>,
    key: Name? = null,
): MetaDelegate<List<T>> = listOfReadable(converter, key)

@DFExperimental
public inline fun <reified T> Meta.listOfSerializable(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<List<T>> = listOfReadable(MetaConverter.serializable(descriptor), key)

/**
 * A property delegate that uses custom key
 */
public fun MetaProvider.value(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<Value?> = object : MetaDelegate<Value?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Value? = get(key ?: Name.of(property.name))?.value

    override val descriptor: MetaDescriptor? = descriptor
}

public fun <R> MetaProvider.value(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
    reader: (Value?) -> R,
): MetaDelegate<R> = object : MetaDelegate<R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R = reader(get(key ?: Name.of(property.name))?.value)

    override val descriptor: MetaDescriptor? = descriptor
}

//TODO add caching for sealed nodes

/* Read-only delegates for [Meta] */

public fun MetaProvider.string(key: Name? = null): MetaDelegate<String?> = value(key = key) { it?.string }

public fun MetaProvider.boolean(key: Name? = null): MetaDelegate<Boolean?> = value(key = key) { it?.boolean }

public fun MetaProvider.number(key: Name? = null): MetaDelegate<Number?> = value(key = key) { it?.numberOrNull }

public fun MetaProvider.double(key: Name? = null): MetaDelegate<Double?> = value(key = key) { it?.double }

public fun MetaProvider.float(key: Name? = null): MetaDelegate<Float?> = value(key = key) { it?.float }

public fun MetaProvider.int(key: Name? = null): MetaDelegate<Int?> = value(key = key) { it?.int }

public fun MetaProvider.long(key: Name? = null): MetaDelegate<Long?> = value(key = key) { it?.long }

public fun MetaProvider.string(default: String, key: Name? = null): MetaDelegate<String> =
    value(key = key) { it?.string ?: default }

public fun MetaProvider.boolean(default: Boolean, key: Name? = null): MetaDelegate<Boolean> =
    value(key = key) { it?.boolean ?: default }

public fun MetaProvider.number(default: Number, key: Name? = null): MetaDelegate<Number> =
    value(key = key) { it?.numberOrNull ?: default }

public fun MetaProvider.double(default: Double, key: Name? = null): MetaDelegate<Double> =
    value(key = key) { it?.double ?: default }

public fun MetaProvider.float(default: Float, key: Name? = null): MetaDelegate<Float> =
    value(key = key) { it?.float ?: default }

public fun MetaProvider.int(default: Int, key: Name? = null): MetaDelegate<Int> =
    value(key = key) { it?.int ?: default }

public fun MetaProvider.long(default: Long, key: Name? = null): MetaDelegate<Long> =
    value(key = key) { it?.long ?: default }

public inline fun <reified E : Enum<E>> MetaProvider.enum(default: E, key: Name? = null): MetaDelegate<E> =
    value<E>(key = key) { it?.enum<E>() ?: default }

public fun MetaProvider.string(key: Name? = null, default: () -> String): MetaDelegate<String> =
    value(key = key) { it?.string ?: default() }

public fun MetaProvider.boolean(key: Name? = null, default: () -> Boolean): MetaDelegate<Boolean> =
    value(key = key) { it?.boolean ?: default() }

public fun MetaProvider.number(key: Name? = null, default: () -> Number): MetaDelegate<Number> =
    value(key = key) { it?.numberOrNull ?: default() }

/**
 * Creates a [MetaDelegate] for an [Instant] value using a specified key and default value provider.
 *
 * @param key The optional [Name] key used to locate the meta value. If null, the property's name will be used as the key.
 * @param default A function that provides the default [Instant] value when the meta value is null or not found.
 * @return A [MetaDelegate] that resolves to an [Instant] value. The [Instant] is resolved from the meta value if available,
 *         or the value provided by the [default] function when necessary.
 *         Throws an error if the meta value type is not compatible with [Instant].
 */
public fun MetaProvider.instant(
    key: Name? = null,
    default: () -> Instant
): MetaDelegate<Instant> = value(key = key) {
    when (it?.type) {
        null, ValueType.NULL,  -> default()
        ValueType.NUMBER -> Instant.fromEpochMilliseconds(it.number.toLong())
        ValueType.STRING -> Instant.parse(it.string)
        ValueType.BOOLEAN -> error("Instant cannot be represented as a boolean")
        ValueType.LIST -> error("Instant cannot be represented as a list")
    }
}

/**
 * Provides a delegated property of type Duration, which retrieves its value from a MetaProvider.
 * If the value is not present or cannot be interpreted as a Duration, the provided default function is used.
 *
 * @param key The key identifying the value within the MetaProvider. Defaults to the property name if null.
 * @param default A lambda providing the default value when the key is not found or the value is null/invalid.
 * @return A MetaDelegate for the requested Duration value.
 */
public fun MetaProvider.duration(
    key: Name? = null,
    default: () -> Duration
): MetaDelegate<Duration> = value(key = key) {
    when (it?.type) {
        null, ValueType.NULL,  -> default()
        ValueType.NUMBER -> it.double.seconds
        ValueType.STRING -> Duration.parse(it.string)
        ValueType.BOOLEAN -> error("Duration cannot be represented as a boolean")
        ValueType.LIST -> error("Duration cannot be represented as a list")
    }
}