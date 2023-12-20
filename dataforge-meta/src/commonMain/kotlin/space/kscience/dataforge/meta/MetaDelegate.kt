package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.transformations.MetaConverter
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.ReadOnlyProperty

/* Meta delegates */

public fun MetaProvider.node(key: Name? = null): ReadOnlyProperty<Any?, Meta?> = ReadOnlyProperty { _, property ->
    get(key ?: property.name.asName())
}

/**
 * Use [converter] to read the Meta node
 */
public fun <T> MetaProvider.convertable(
    converter: MetaConverter<T>,
    key: Name? = null,
): ReadOnlyProperty<Any?, T?> = ReadOnlyProperty { _, property ->
    get(key ?: property.name.asName())?.let { converter.metaToObject(it) }
}

/**
 * Use object serializer to transform it to Meta and back
 */
@DFExperimental
public inline fun <reified T> MetaProvider.serializable(
    descriptor: MetaDescriptor? = null,
    key: Name? = null,
): ReadOnlyProperty<Any?, T?> = convertable(MetaConverter.serializable(descriptor), key)

@Deprecated("Use convertable", ReplaceWith("convertable(converter, key)"))
public fun <T> MetaProvider.node(
    key: Name? = null,
    converter: MetaConverter<T>,
): ReadOnlyProperty<Any?, T?> = convertable(converter, key)

/**
 * Use [converter] to convert a list of same name siblings meta to object
 */
public fun <T> Meta.listOfConvertable(
    converter: MetaConverter<T>,
    key: Name? = null,
): ReadOnlyProperty<Any?, List<T>> = ReadOnlyProperty{_, property ->
    val name = key ?: property.name.asName()
    getIndexed(name).values.map { converter.metaToObject(it) }
}

@DFExperimental
public inline fun <reified T> Meta.listOfSerializable(
    descriptor: MetaDescriptor? = null,
    key: Name? = null,
): ReadOnlyProperty<Any?, List<T>> = listOfConvertable(MetaConverter.serializable(descriptor), key)

/**
 * A property delegate that uses custom key
 */
public fun MetaProvider.value(key: Name? = null): ReadOnlyProperty<Any?, Value?> = ReadOnlyProperty { _, property ->
    get(key ?: property.name.asName())?.value
}

public fun <R> MetaProvider.value(
    key: Name? = null,
    reader: (Value?) -> R,
): ReadOnlyProperty<Any?, R> = ReadOnlyProperty { _, property ->
    reader(get(key ?: property.name.asName())?.value)
}

//TODO add caching for sealed nodes

/* Read-only delegates for [Meta] */

public fun MetaProvider.string(key: Name? = null): ReadOnlyProperty<Any?, String?> = value(key) { it?.string }

public fun MetaProvider.boolean(key: Name? = null): ReadOnlyProperty<Any?, Boolean?> = value(key) { it?.boolean }

public fun MetaProvider.number(key: Name? = null): ReadOnlyProperty<Any?, Number?> = value(key) { it?.numberOrNull }

public fun MetaProvider.double(key: Name? = null): ReadOnlyProperty<Any?, Double?> = value(key) { it?.double }

public fun MetaProvider.float(key: Name? = null): ReadOnlyProperty<Any?, Float?> = value(key) { it?.float }

public fun MetaProvider.int(key: Name? = null): ReadOnlyProperty<Any?, Int?> = value(key) { it?.int }

public fun MetaProvider.long(key: Name? = null): ReadOnlyProperty<Any?, Long?> = value(key) { it?.long }

public fun MetaProvider.string(default: String, key: Name? = null): ReadOnlyProperty<Any?, String> =
    value(key) { it?.string ?: default }

public fun MetaProvider.boolean(default: Boolean, key: Name? = null): ReadOnlyProperty<Any?, Boolean> =
    value(key) { it?.boolean ?: default }

public fun MetaProvider.number(default: Number, key: Name? = null): ReadOnlyProperty<Any?, Number> =
    value(key) { it?.numberOrNull ?: default }

public fun MetaProvider.double(default: Double, key: Name? = null): ReadOnlyProperty<Any?, Double> =
    value(key) { it?.double ?: default }

public fun MetaProvider.float(default: Float, key: Name? = null): ReadOnlyProperty<Any?, Float> =
    value(key) { it?.float ?: default }

public fun MetaProvider.int(default: Int, key: Name? = null): ReadOnlyProperty<Any?, Int> =
    value(key) { it?.int ?: default }

public fun MetaProvider.long(default: Long, key: Name? = null): ReadOnlyProperty<Any?, Long> =
    value(key) { it?.long ?: default }

public inline fun <reified E : Enum<E>> MetaProvider.enum(default: E, key: Name? = null): ReadOnlyProperty<Any?, E> =
    value<E>(key) { it?.enum<E>() ?: default }

public fun MetaProvider.string(key: Name? = null, default: () -> String): ReadOnlyProperty<Any?, String> =
    value(key) { it?.string ?: default() }

public fun MetaProvider.boolean(key: Name? = null, default: () -> Boolean): ReadOnlyProperty<Any?, Boolean> =
    value(key) { it?.boolean ?: default() }

public fun MetaProvider.number(key: Name? = null, default: () -> Number): ReadOnlyProperty<Any?, Number> =
    value(key) { it?.numberOrNull ?: default() }
