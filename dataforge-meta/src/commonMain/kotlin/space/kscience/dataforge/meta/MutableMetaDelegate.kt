package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.transformations.MetaConverter
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.values.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Read-write delegates */

public typealias MutableMetaDelegate = ReadWriteProperty<Any?, Meta?>

public fun MutableMetaProvider.item(key: Name? = null): MutableMetaDelegate = object : MutableMetaDelegate {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Meta? {
        return getMeta(key ?: property.name.asName())
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Meta?) {
        val name = key ?: property.name.asName()
        setMeta(name, value)
    }
}

/* Mutable converters */

/**
 * A type converter for a [MutableMetaDelegate]
 */
public fun <R : Any> MutableMetaDelegate.convert(
    converter: MetaConverter<R>,
): ReadWriteProperty<Any?, R?> = object : ReadWriteProperty<Any?, R?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R? =
        this@convert.getValue(thisRef, property)?.let(converter::metaToObject)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R?) {
        val item = value?.let(converter::objectToMeta)
        this@convert.setValue(thisRef, property, item)
    }
}

public fun <R : Any> MutableMetaDelegate.convert(
    converter: MetaConverter<R>,
    default: () -> R,
): ReadWriteProperty<Any?, R> = object : ReadWriteProperty<Any?, R> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property)?.let(converter::metaToObject) ?: default()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        val item = value.let(converter::objectToMeta)
        this@convert.setValue(thisRef, property, item)
    }
}

public fun <R> MutableMetaDelegate.convert(
    reader: (Meta?) -> R,
    writer: (R) -> Meta?,
): ReadWriteProperty<Any?, R> = object : ReadWriteProperty<Any?, R> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): R =
        this@convert.getValue(thisRef, property).let(reader)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        val item = value?.let(writer)
        this@convert.setValue(thisRef, property, item)
    }
}


/* Read-write delegates for [MutableItemProvider] */

/**
 * A property delegate that uses custom key
 */
public fun MutableMetaProvider.value(key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

public fun MutableMetaProvider.string(key: Name? = null): ReadWriteProperty<Any?, String?> =
    item(key).convert(MetaConverter.string)

public fun MutableMetaProvider.boolean(key: Name? = null): ReadWriteProperty<Any?, Boolean?> =
    item(key).convert(MetaConverter.boolean)

public fun MutableMetaProvider.number(key: Name? = null): ReadWriteProperty<Any?, Number?> =
    item(key).convert(MetaConverter.number)

public fun MutableMetaProvider.string(default: String, key: Name? = null): ReadWriteProperty<Any?, String> =
    item(key).convert(MetaConverter.string) { default }

public fun MutableMetaProvider.boolean(default: Boolean, key: Name? = null): ReadWriteProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean) { default }

public fun MutableMetaProvider.number(default: Number, key: Name? = null): ReadWriteProperty<Any?, Number> =
    item(key).convert(MetaConverter.number) { default }

public fun MutableMetaProvider.value(key: Name? = null, default: () -> Value): ReadWriteProperty<Any?, Value> =
    item(key).convert(MetaConverter.value, default)

public fun MutableMetaProvider.string(key: Name? = null, default: () -> String): ReadWriteProperty<Any?, String> =
    item(key).convert(MetaConverter.string, default)

public fun MutableMetaProvider.boolean(key: Name? = null, default: () -> Boolean): ReadWriteProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean, default)

public fun MutableMetaProvider.number(key: Name? = null, default: () -> Number): ReadWriteProperty<Any?, Number> =
    item(key).convert(MetaConverter.number, default)

public inline fun <reified E : Enum<E>> MutableMetaProvider.enum(
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, E> =
    item(key).convert(MetaConverter.enum()) { default }

/* Number delegates */

public fun MutableMetaProvider.int(key: Name? = null): ReadWriteProperty<Any?, Int?> =
    item(key).convert(MetaConverter.int)

public fun MutableMetaProvider.double(key: Name? = null): ReadWriteProperty<Any?, Double?> =
    item(key).convert(MetaConverter.double)

public fun MutableMetaProvider.long(key: Name? = null): ReadWriteProperty<Any?, Long?> =
    item(key).convert(MetaConverter.long)

public fun MutableMetaProvider.float(key: Name? = null): ReadWriteProperty<Any?, Float?> =
    item(key).convert(MetaConverter.float)


/* Safe number delegates*/

public fun MutableMetaProvider.int(default: Int, key: Name? = null): ReadWriteProperty<Any?, Int> =
    item(key).convert(MetaConverter.int) { default }

public fun MutableMetaProvider.double(default: Double, key: Name? = null): ReadWriteProperty<Any?, Double> =
    item(key).convert(MetaConverter.double) { default }

public fun MutableMetaProvider.long(default: Long, key: Name? = null): ReadWriteProperty<Any?, Long> =
    item(key).convert(MetaConverter.long) { default }

public fun MutableMetaProvider.float(default: Float, key: Name? = null): ReadWriteProperty<Any?, Float> =
    item(key).convert(MetaConverter.float) { default }


/* Extra delegates for special cases */

public fun MutableMetaProvider.stringList(
    vararg default: String,
    key: Name? = null,
): ReadWriteProperty<Any?, List<String>> = item(key).convert(
    reader = { it?.stringList ?: listOf(*default) },
    writer = { Meta(it.map { str -> str.asValue() }.asValue()) }
)

public fun MutableMetaProvider.stringList(
    key: Name? = null,
): ReadWriteProperty<Any?, List<String>?> = item(key).convert(
    reader = { it?.stringList },
    writer = { it?.map { str -> str.asValue() }?.asValue()?.let { Meta(it) } }
)

public fun MutableMetaProvider.numberList(
    vararg default: Number,
    key: Name? = null,
): ReadWriteProperty<Any?, List<Number>> = item(key).convert(
    reader = { it?.value?.list?.map { value -> value.numberOrNull ?: Double.NaN } ?: listOf(*default) },
    writer = { Meta(it.map { num -> num.asValue() }.asValue()) }
)

/* A special delegate for double arrays */


public fun MutableMetaProvider.doubleArray(
    vararg default: Double,
    key: Name? = null,
): ReadWriteProperty<Any?, DoubleArray> = item(key).convert(
    reader = { it?.value?.doubleArray ?: doubleArrayOf(*default) },
    writer = { Meta(DoubleArrayValue(it)) }
)

public fun <T> MutableMetaProvider.listValue(
    key: Name? = null,
    writer: (T) -> Value = { Value.of(it) },
    reader: (Value) -> T,
): ReadWriteProperty<Any?, List<T>?> = item(key).convert(MetaConverter.valueList(writer, reader))
