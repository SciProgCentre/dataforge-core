package hep.dataforge.meta

import hep.dataforge.meta.transformations.MetaConverter
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.DoubleArrayValue
import hep.dataforge.values.Value
import hep.dataforge.values.asValue
import hep.dataforge.values.doubleArray
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Read-write delegates */

public typealias MutableItemDelegate = ReadWriteProperty<Any?, MetaItem<*>?>

public fun MutableItemProvider.item(key: Name? = null): MutableItemDelegate = object : MutableItemDelegate {
    override fun getValue(thisRef: Any?, property: KProperty<*>): MetaItem<*>? {
        return getItem(key ?: property.name.asName())
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MetaItem<*>?) {
        val name = key ?: property.name.asName()
        setItem(name, value)
    }
}

/* Mutable converters */

/**
 * A type converter for a mutable [MetaItem] delegate
 */
public fun <R : Any> MutableItemDelegate.convert(
    converter: MetaConverter<R>,
): ReadWriteProperty<Any?, R?> = object : ReadWriteProperty<Any?, R?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R? =
        this@convert.getValue(thisRef, property)?.let(converter::itemToObject)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R?) {
        val item = value?.let(converter::objectToMetaItem)
        this@convert.setValue(thisRef, property, item)
    }
}

public fun <R : Any> MutableItemDelegate.convert(
    converter: MetaConverter<R>,
    default: () -> R,
): ReadWriteProperty<Any?, R> = object : ReadWriteProperty<Any?, R> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property)?.let(converter::itemToObject) ?: default()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        val item = value.let(converter::objectToMetaItem)
        this@convert.setValue(thisRef, property, item)
    }
}

public fun <R> MutableItemDelegate.convert(
    reader: (MetaItem<*>?) -> R,
    writer: (R) -> MetaItem<*>?,
): ReadWriteProperty<Any?, R> = object : ReadWriteProperty<Any?, R> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property).let(reader)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        val item = value?.let(writer)
        this@convert.setValue(thisRef, property, item)
    }
}

/* Read-write delegates */

/**
 * A property delegate that uses custom key
 */
public fun MutableItemProvider.value(key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

public fun MutableItemProvider.string(key: Name? = null): ReadWriteProperty<Any?, String?> =
    item(key).convert(MetaConverter.string)

public fun MutableItemProvider.boolean(key: Name? = null): ReadWriteProperty<Any?, Boolean?> =
    item(key).convert(MetaConverter.boolean)

public fun MutableItemProvider.number(key: Name? = null): ReadWriteProperty<Any?, Number?> =
    item(key).convert(MetaConverter.number)

public fun MutableItemProvider.string(default: String, key: Name? = null): ReadWriteProperty<Any?, String> =
    item(key).convert(MetaConverter.string) { default }

public fun MutableItemProvider.boolean(default: Boolean, key: Name? = null): ReadWriteProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean) { default }

public fun MutableItemProvider.number(default: Number, key: Name? = null): ReadWriteProperty<Any?, Number> =
    item(key).convert(MetaConverter.number) { default }

public fun MutableItemProvider.value(key: Name? = null, default: () -> Value): ReadWriteProperty<Any?, Value> =
    item(key).convert(MetaConverter.value, default)

public fun MutableItemProvider.string(key: Name? = null, default: () -> String): ReadWriteProperty<Any?, String> =
    item(key).convert(MetaConverter.string, default)

public fun MutableItemProvider.boolean(key: Name? = null, default: () -> Boolean): ReadWriteProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean, default)

public fun MutableItemProvider.number(key: Name? = null, default: () -> Number): ReadWriteProperty<Any?, Number> =
    item(key).convert(MetaConverter.number, default)

public inline fun <reified E : Enum<E>> MutableItemProvider.enum(
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, E> =
    item(key).convert(MetaConverter.enum()) { default }

public inline fun <reified M : MutableMeta<M>> M.node(key: Name? = null): ReadWriteProperty<Any?, M?> =
    item(key).convert(reader = { it?.let { it.node as M } }, writer = { it?.let { MetaItem.NodeItem(it) } })


public fun Configurable.value(key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

/* Number delegates */

public fun MutableItemProvider.int(key: Name? = null): ReadWriteProperty<Any?, Int?> =
    item(key).convert(MetaConverter.int)

public fun MutableItemProvider.double(key: Name? = null): ReadWriteProperty<Any?, Double?> =
    item(key).convert(MetaConverter.double)

public fun MutableItemProvider.long(key: Name? = null): ReadWriteProperty<Any?, Long?> =
    item(key).convert(MetaConverter.long)

public fun MutableItemProvider.float(key: Name? = null): ReadWriteProperty<Any?, Float?> =
    item(key).convert(MetaConverter.float)


/* Safe number delegates*/

public fun MutableItemProvider.int(default: Int, key: Name? = null): ReadWriteProperty<Any?, Int> =
    item(key).convert(MetaConverter.int) { default }

public fun MutableItemProvider.double(default: Double, key: Name? = null): ReadWriteProperty<Any?, Double> =
    item(key).convert(MetaConverter.double) { default }

public fun MutableItemProvider.long(default: Long, key: Name? = null): ReadWriteProperty<Any?, Long> =
    item(key).convert(MetaConverter.long) { default }

public fun MutableItemProvider.float(default: Float, key: Name? = null): ReadWriteProperty<Any?, Float> =
    item(key).convert(MetaConverter.float) { default }


/* Extra delegates for special cases */

public fun MutableItemProvider.stringList(
    vararg default: String,
    key: Name? = null,
): ReadWriteProperty<Any?, List<String>> = item(key).convert(
    reader = { it?.stringList ?: listOf(*default) },
    writer = { it.map { str -> str.asValue() }.asValue().asMetaItem() }
)

public fun MutableItemProvider.stringList(
    key: Name? = null,
): ReadWriteProperty<Any?, List<String>?> = item(key).convert(
    reader = { it?.stringList },
    writer = { it?.map { str -> str.asValue() }?.asValue()?.asMetaItem() }
)

public fun MutableItemProvider.numberList(
    vararg default: Number,
    key: Name? = null,
): ReadWriteProperty<Any?, List<Number>> = item(key).convert(
    reader = { it?.value?.list?.map { value -> value.number } ?: listOf(*default) },
    writer = { it.map { num -> num.asValue() }.asValue().asMetaItem() }
)

/* A special delegate for double arrays */


public fun MutableItemProvider.doubleArray(
    vararg default: Double,
    key: Name? = null,
): ReadWriteProperty<Any?, DoubleArray> = item(key).convert(
    reader = { it?.value?.doubleArray ?: doubleArrayOf(*default) },
    writer = { DoubleArrayValue(it).asMetaItem() }
)

public fun <T> MutableItemProvider.listValue(
    key: Name? = null,
    writer: (T) -> Value = { Value.of(it) },
    reader: (Value) -> T,
): ReadWriteProperty<Any?, List<T>?> = item(key).convert(MetaConverter.valueList(writer, reader))
