package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Read-write delegates */

public fun MutableMetaProvider.node(key: Name? = null): ReadWriteProperty<Any?, Meta?> =
    object : ReadWriteProperty<Any?, Meta?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Meta? {
            return get(key ?: property.name.asName())
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Meta?) {
            val name = key ?: property.name.asName()
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
): ReadWriteProperty<Any?, T?> =
    object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val name = key ?: property.name.asName()
            return get(name)?.let { converter.read(it) }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            val name = key ?: property.name.asName()
            set(name, value?.let { converter.convert(it) })
        }
    }

@Deprecated("Use convertable", ReplaceWith("convertable(converter, key)"))
public fun <T> MutableMetaProvider.node(key: Name? = null, converter: MetaConverter<T>): ReadWriteProperty<Any?, T?> =
    convertable(converter, key)

/**
 * Use object serializer to transform it to Meta and back.
 * Note that mutation of the object does not change Meta.
 */
@DFExperimental
public inline fun <reified T> MutableMetaProvider.serializable(
    descriptor: MetaDescriptor? = null,
    key: Name? = null,
): ReadWriteProperty<Any?, T?> = convertable(MetaConverter.serializable(descriptor), key)

/**
 * Use [converter] to convert a list of same name siblings meta to object and back.
 * Note that mutation of the object does not change Meta.
 */
public fun <T> MutableMeta.listOfConvertable(
    converter: MetaConverter<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, List<T>> = object : ReadWriteProperty<Any?, List<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: property.name.asName()
        return getIndexed(name).values.map { converter.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        val name = key ?: property.name.asName()
        setIndexed(name, value.map { converter.convert(it) })
    }
}

@DFExperimental
public inline fun <reified T> MutableMeta.listOfSerializable(
    descriptor: MetaDescriptor? = null,
    key: Name? = null,
): ReadWriteProperty<Any?, List<T>> = listOfConvertable(MetaConverter.serializable(descriptor), key)


public fun MutableMetaProvider.value(key: Name? = null): ReadWriteProperty<Any?, Value?> =
    object : ReadWriteProperty<Any?, Value?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Value? =
            get(key ?: property.name.asName())?.value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Value?) {
            setValue(key ?: property.name.asName(), value)
        }
    }

public fun <T> MutableMetaProvider.value(
    key: Name? = null,
    writer: (T) -> Value? = { Value.of(it) },
    reader: (Value?) -> T,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        reader(get(key ?: property.name.asName())?.value)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setValue(key ?: property.name.asName(), writer(value))
    }
}

/* Read-write delegates for [MutableItemProvider] */

public fun MutableMetaProvider.string(key: Name? = null): ReadWriteProperty<Any?, String?> =
    value(key) { it?.string }

public fun MutableMetaProvider.boolean(key: Name? = null): ReadWriteProperty<Any?, Boolean?> =
    value(key) { it?.boolean }

public fun MutableMetaProvider.number(key: Name? = null): ReadWriteProperty<Any?, Number?> =
    value(key) { it?.number }

public fun MutableMetaProvider.string(default: String, key: Name? = null): ReadWriteProperty<Any?, String> =
    value(key) { it?.string ?: default }

public fun MutableMetaProvider.boolean(default: Boolean, key: Name? = null): ReadWriteProperty<Any?, Boolean> =
    value(key) { it?.boolean ?: default }

public fun MutableMetaProvider.number(default: Number, key: Name? = null): ReadWriteProperty<Any?, Number> =
    value(key) { it?.number ?: default }

public fun MutableMetaProvider.string(key: Name? = null, default: () -> String): ReadWriteProperty<Any?, String> =
    value(key) { it?.string ?: default() }

public fun MutableMetaProvider.boolean(key: Name? = null, default: () -> Boolean): ReadWriteProperty<Any?, Boolean> =
    value(key) { it?.boolean ?: default() }

public fun MutableMetaProvider.number(key: Name? = null, default: () -> Number): ReadWriteProperty<Any?, Number> =
    value(key) { it?.number ?: default() }

public inline fun <reified E : Enum<E>> MutableMetaProvider.enum(
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, E> = value(key) { value -> value?.string?.let { enumValueOf<E>(it) } ?: default }

/* Number delegates */

public fun MutableMetaProvider.int(key: Name? = null): ReadWriteProperty<Any?, Int?> =
    value(key) { it?.int }

public fun MutableMetaProvider.double(key: Name? = null): ReadWriteProperty<Any?, Double?> =
    value(key) { it?.double }

public fun MutableMetaProvider.long(key: Name? = null): ReadWriteProperty<Any?, Long?> =
    value(key) { it?.long }

public fun MutableMetaProvider.float(key: Name? = null): ReadWriteProperty<Any?, Float?> =
    value(key) { it?.float }


/* Safe number delegates*/

public fun MutableMetaProvider.int(default: Int, key: Name? = null): ReadWriteProperty<Any?, Int> =
    value(key) { it?.int ?: default }

public fun MutableMetaProvider.double(default: Double, key: Name? = null): ReadWriteProperty<Any?, Double> =
    value(key) { it?.double ?: default }

public fun MutableMetaProvider.long(default: Long, key: Name? = null): ReadWriteProperty<Any?, Long> =
    value(key) { it?.long ?: default }

public fun MutableMetaProvider.float(default: Float, key: Name? = null): ReadWriteProperty<Any?, Float> =
    value(key) { it?.float ?: default }


/* Extra delegates for special cases */

public fun MutableMetaProvider.stringList(
    vararg default: String,
    key: Name? = null,
): ReadWriteProperty<Any?, List<String>> = value(
    key,
    writer = { list -> list.map { str -> str.asValue() }.asValue() },
    reader = { it?.stringList ?: listOf(*default) },
)

public fun MutableMetaProvider.stringList(
    key: Name? = null,
): ReadWriteProperty<Any?, List<String>?> = value(
    key,
    writer = { it -> it?.map { str -> str.asValue() }?.asValue() },
    reader = { it?.stringList },
)

public fun MutableMetaProvider.numberList(
    vararg default: Number,
    key: Name? = null,
): ReadWriteProperty<Any?, List<Number>> = value(
    key,
    writer = { it.map { num -> num.asValue() }.asValue() },
    reader = { it?.list?.map { value -> value.numberOrNull ?: Double.NaN } ?: listOf(*default) },
)

/* A special delegate for double arrays */


public fun MutableMetaProvider.doubleArray(
    vararg default: Double,
    key: Name? = null,
): ReadWriteProperty<Any?, DoubleArray> = value(
    key,
    writer = { DoubleArrayValue(it) },
    reader = { it?.doubleArray ?: doubleArrayOf(*default) },
)

public fun <T> MutableMetaProvider.listValue(
    key: Name? = null,
    writer: (T) -> Value = { Value.of(it) },
    reader: (Value) -> T,
): ReadWriteProperty<Any?, List<T>?> = value(
    key,
    writer = { it?.map(writer)?.asValue() },
    reader = { it?.list?.map(reader) }
)
