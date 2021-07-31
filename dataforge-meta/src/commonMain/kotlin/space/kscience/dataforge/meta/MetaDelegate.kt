package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.transformations.MetaConverter
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.values.Value
import kotlin.properties.ReadOnlyProperty

/* Meta delegates */

public typealias MetaDelegate = ReadOnlyProperty<Any?, Meta?>

public fun MetaProvider.item(key: Name? = null): MetaDelegate = ReadOnlyProperty { _, property ->
    getMeta(key ?: property.name.asName())
}

//TODO add caching for sealed nodes


/**
 * Apply a converter to this delegate creating a delegate with a custom type
 */
public fun <R : Any> MetaDelegate.convert(
    converter: MetaConverter<R>,
): ReadOnlyProperty<Any?, R?> = ReadOnlyProperty { thisRef, property ->
    this@convert.getValue(thisRef, property)?.let(converter::metaToObject)
}

/*
 *
 */
public fun <R : Any> MetaDelegate.convert(
    converter: MetaConverter<R>,
    default: () -> R,
): ReadOnlyProperty<Any?, R> = ReadOnlyProperty<Any?, R> { thisRef, property ->
    this@convert.getValue(thisRef, property)?.let(converter::metaToObject) ?: default()
}

/**
 * A converter with a custom reader transformation
 */
public fun <R> MetaDelegate.convert(
    reader: (Meta?) -> R,
): ReadOnlyProperty<Any?, R> = ReadOnlyProperty<Any?, R> { thisRef, property ->
    this@convert.getValue(thisRef, property).let(reader)
}

/* Read-only delegates for [Meta] */

/**
 * A property delegate that uses custom key
 */
public fun MetaProvider.value(key: Name? = null): ReadOnlyProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

public fun MetaProvider.string(key: Name? = null): ReadOnlyProperty<Any?, String?> =
    item(key).convert(MetaConverter.string)

public fun MetaProvider.boolean(key: Name? = null): ReadOnlyProperty<Any?, Boolean?> =
    item(key).convert(MetaConverter.boolean)

public fun MetaProvider.number(key: Name? = null): ReadOnlyProperty<Any?, Number?> =
    item(key).convert(MetaConverter.number)

public fun MetaProvider.double(key: Name? = null): ReadOnlyProperty<Any?, Double?> =
    item(key).convert(MetaConverter.double)

public fun MetaProvider.float(key: Name? = null): ReadOnlyProperty<Any?, Float?> =
    item(key).convert(MetaConverter.float)

public fun MetaProvider.int(key: Name? = null): ReadOnlyProperty<Any?, Int?> =
    item(key).convert(MetaConverter.int)

public fun MetaProvider.long(key: Name? = null): ReadOnlyProperty<Any?, Long?> =
    item(key).convert(MetaConverter.long)

public fun MetaProvider.node(key: Name? = null): ReadOnlyProperty<Any?, Meta?> =
    item(key).convert(MetaConverter.meta)

public fun MetaProvider.string(default: String, key: Name? = null): ReadOnlyProperty<Any?, String> =
    item(key).convert(MetaConverter.string) { default }

public fun MetaProvider.boolean(default: Boolean, key: Name? = null): ReadOnlyProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean) { default }

public fun MetaProvider.number(default: Number, key: Name? = null): ReadOnlyProperty<Any?, Number> =
    item(key).convert(MetaConverter.number) { default }

public fun MetaProvider.double(default: Double, key: Name? = null): ReadOnlyProperty<Any?, Double> =
    item(key).convert(MetaConverter.double) { default }

public fun MetaProvider.float(default: Float, key: Name? = null): ReadOnlyProperty<Any?, Float> =
    item(key).convert(MetaConverter.float) { default }

public fun MetaProvider.int(default: Int, key: Name? = null): ReadOnlyProperty<Any?, Int> =
    item(key).convert(MetaConverter.int) { default }

public fun MetaProvider.long(default: Long, key: Name? = null): ReadOnlyProperty<Any?, Long> =
    item(key).convert(MetaConverter.long) { default }

public inline fun <reified E : Enum<E>> MetaProvider.enum(default: E, key: Name? = null): ReadOnlyProperty<Any?, E> =
    item(key).convert(MetaConverter.enum()) { default }

public fun MetaProvider.string(key: Name? = null, default: () -> String): ReadOnlyProperty<Any?, String> =
    item(key).convert(MetaConverter.string, default)

public fun MetaProvider.boolean(key: Name? = null, default: () -> Boolean): ReadOnlyProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean, default)

public fun MetaProvider.number(key: Name? = null, default: () -> Number): ReadOnlyProperty<Any?, Number> =
    item(key).convert(MetaConverter.number, default)
