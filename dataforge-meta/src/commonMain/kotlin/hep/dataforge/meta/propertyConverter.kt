package hep.dataforge.meta

import hep.dataforge.meta.transformations.MetaConverter
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Apply a converter to this delegate creating a delegate with a custom type
 */
fun <R : Any> ItemDelegate.convert(
    converter: MetaConverter<R>
): ReadOnlyProperty<Any?, R?> = object : ReadOnlyProperty<Any?, R?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R? =
        this@convert.getValue(thisRef, property)?.let(converter::itemToObject)
}

/*
 *
 */
fun <R : Any> ItemDelegate.convert(
    converter: MetaConverter<R>,
    default: () -> R
): ReadOnlyProperty<Any?, R> = object : ReadOnlyProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property)?.let(converter::itemToObject) ?: default()
}

/**
 * A converter with a custom reader transformation
 */
fun <R> ItemDelegate.convert(
    reader: (MetaItem<*>?) -> R
): ReadOnlyProperty<Any?, R> = object : ReadOnlyProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property).let(reader)
}

/*Mutable converters*/

/**
 * A type converter for a mutable [MetaItem] delegate
 */
fun <R : Any> MutableItemDelegate.convert(
    converter: MetaConverter<R>
): ReadWriteProperty<Any?, R?> = object : ReadWriteProperty<Any?, R?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R? =
        this@convert.getValue(thisRef, property)?.let(converter::itemToObject)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R?) {
        val item = value?.let(converter::objectToMetaItem)
        this@convert.setValue(thisRef, property, item)
    }
}

fun <R : Any> MutableItemDelegate.convert(
    converter: MetaConverter<R>,
    default: () -> R
): ReadWriteProperty<Any?, R> = object : ReadWriteProperty<Any?, R> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property)?.let(converter::itemToObject) ?: default()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        val item = value.let(converter::objectToMetaItem)
        this@convert.setValue(thisRef, property, item)
    }
}

fun <R> MutableItemDelegate.convert(
    reader: (MetaItem<*>?) -> R,
    writer: (R) -> MetaItem<*>?
): ReadWriteProperty<Any?, R> = object : ReadWriteProperty<Any?, R> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property).let(reader)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        val item = value?.let(writer)
        this@convert.setValue(thisRef, property, item)
    }
}
