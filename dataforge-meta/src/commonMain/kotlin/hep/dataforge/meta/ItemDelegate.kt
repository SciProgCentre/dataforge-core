package hep.dataforge.meta

import hep.dataforge.meta.transformations.MetaConverter
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.Value
import kotlin.properties.ReadOnlyProperty

/* Meta delegates */

public typealias ItemDelegate = ReadOnlyProperty<Any?, MetaItem<*>?>

public fun ItemProvider.item(key: Name? = null): ItemDelegate = ReadOnlyProperty { _, property ->
    getItem(key ?: property.name.asName())
}

//TODO add caching for sealed nodes


/**
 * Apply a converter to this delegate creating a delegate with a custom type
 */
public fun <R : Any> ItemDelegate.convert(
    converter: MetaConverter<R>,
): ReadOnlyProperty<Any?, R?> = ReadOnlyProperty { thisRef, property ->
    this@convert.getValue(thisRef, property)?.let(converter::itemToObject)
}

/*
 *
 */
public fun <R : Any> ItemDelegate.convert(
    converter: MetaConverter<R>,
    default: () -> R,
): ReadOnlyProperty<Any?, R> = ReadOnlyProperty<Any?, R> { thisRef, property ->
    this@convert.getValue(thisRef, property)?.let(converter::itemToObject) ?: default()
}

/**
 * A converter with a custom reader transformation
 */
public fun <R> ItemDelegate.convert(
    reader: (MetaItem<*>?) -> R,
): ReadOnlyProperty<Any?, R> = ReadOnlyProperty<Any?, R> { thisRef, property ->
    this@convert.getValue(thisRef, property).let(reader)
}

/* Read-only delegates for Metas */

/**
 * A property delegate that uses custom key
 */
public fun ItemProvider.value(key: Name? = null): ReadOnlyProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

public fun ItemProvider.string(key: Name? = null): ReadOnlyProperty<Any?, String?> =
    item(key).convert(MetaConverter.string)

public fun ItemProvider.boolean(key: Name? = null): ReadOnlyProperty<Any?, Boolean?> =
    item(key).convert(MetaConverter.boolean)

public fun ItemProvider.number(key: Name? = null): ReadOnlyProperty<Any?, Number?> =
    item(key).convert(MetaConverter.number)

public fun ItemProvider.double(key: Name? = null): ReadOnlyProperty<Any?, Double?> =
    item(key).convert(MetaConverter.double)

public fun ItemProvider.float(key: Name? = null): ReadOnlyProperty<Any?, Float?> =
    item(key).convert(MetaConverter.float)

public fun ItemProvider.int(key: Name? = null): ReadOnlyProperty<Any?, Int?> =
    item(key).convert(MetaConverter.int)

public fun ItemProvider.long(key: Name? = null): ReadOnlyProperty<Any?, Long?> =
    item(key).convert(MetaConverter.long)

public fun ItemProvider.node(key: Name? = null): ReadOnlyProperty<Any?, Meta?> =
    item(key).convert(MetaConverter.meta)

public fun ItemProvider.string(default: String, key: Name? = null): ReadOnlyProperty<Any?, String> =
    item(key).convert(MetaConverter.string) { default }

public fun ItemProvider.boolean(default: Boolean, key: Name? = null): ReadOnlyProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean) { default }

public fun ItemProvider.number(default: Number, key: Name? = null): ReadOnlyProperty<Any?, Number> =
    item(key).convert(MetaConverter.number) { default }

public fun ItemProvider.double(default: Double, key: Name? = null): ReadOnlyProperty<Any?, Double> =
    item(key).convert(MetaConverter.double) { default }

public fun ItemProvider.float(default: Float, key: Name? = null): ReadOnlyProperty<Any?, Float> =
    item(key).convert(MetaConverter.float) { default }

public fun ItemProvider.int(default: Int, key: Name? = null): ReadOnlyProperty<Any?, Int> =
    item(key).convert(MetaConverter.int) { default }

public fun ItemProvider.long(default: Long, key: Name? = null): ReadOnlyProperty<Any?, Long> =
    item(key).convert(MetaConverter.long) { default }

public inline fun <reified E : Enum<E>> ItemProvider.enum(default: E, key: Name? = null): ReadOnlyProperty<Any?, E> =
    item(key).convert(MetaConverter.enum()) { default }

public fun ItemProvider.string(key: Name? = null, default: () -> String): ReadOnlyProperty<Any?, String> =
    item(key).convert(MetaConverter.string, default)

public fun ItemProvider.boolean(key: Name? = null, default: () -> Boolean): ReadOnlyProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean, default)

public fun ItemProvider.number(key: Name? = null, default: () -> Number): ReadOnlyProperty<Any?, Number> =
    item(key).convert(MetaConverter.number, default)
