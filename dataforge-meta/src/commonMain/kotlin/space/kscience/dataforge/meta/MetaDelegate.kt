package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* Meta delegates */

public interface MetaDelegate<T> : ReadOnlyProperty<Any?, T>, Described


public fun MetaProvider.node(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<Meta?> = object : MetaDelegate<Meta?> {
    override val descriptor: MetaDescriptor? = descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): Meta? {
        return get(key ?: property.name.asName())
    }
}

/**
 * Use [metaReader] to read the Meta node
 */
public fun <T> MetaProvider.spec(
    metaReader: MetaReader<T>,
    key: Name? = null,
): MetaDelegate<T?> = object : MetaDelegate<T?> {
    override val descriptor: MetaDescriptor? get() = metaReader.descriptor

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return get(key ?: property.name.asName())?.let { metaReader.read(it) }
    }
}

/**
 * Use object serializer to transform it to Meta and back
 */
@DFExperimental
public inline fun <reified T> MetaProvider.serializable(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<T?> = spec(MetaConverter.serializable(descriptor), key)

@Deprecated("Use convertable", ReplaceWith("convertable(converter, key)"))
public fun <T> MetaProvider.node(
    key: Name? = null,
    converter: MetaReader<T>,
): ReadOnlyProperty<Any?, T?> = spec(converter, key)

/**
 * Use [converter] to convert a list of same name siblings meta to object
 */
public fun <T> Meta.listOfSpec(
    converter: MetaReader<T>,
    key: Name? = null,
): MetaDelegate<List<T>> = object : MetaDelegate<List<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: property.name.asName()
        return getIndexed(name).values.map { converter.read(it) }
    }

    override val descriptor: MetaDescriptor? = converter.descriptor?.copy(multiple = true)
}

@DFExperimental
public inline fun <reified T> Meta.listOfSerializable(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<List<T>> = listOfSpec(MetaConverter.serializable(descriptor), key)

/**
 * A property delegate that uses custom key
 */
public fun MetaProvider.value(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MetaDelegate<Value?> = object : MetaDelegate<Value?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Value? = get(key ?: property.name.asName())?.value

    override val descriptor: MetaDescriptor? = descriptor
}

public fun <R> MetaProvider.value(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
    reader: (Value?) -> R,
): MetaDelegate<R> = object : MetaDelegate<R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R = reader(get(key ?: property.name.asName())?.value)

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
