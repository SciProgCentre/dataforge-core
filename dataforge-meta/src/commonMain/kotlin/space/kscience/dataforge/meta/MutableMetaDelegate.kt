package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.getIndexedList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/* Read-write delegates */

public interface MutableMetaDelegate<T> : ReadWriteProperty<Any?, T>, Described

public fun MutableMetaProvider.node(
    key: Name? = null,
    descriptor: MetaDescriptor? = null,
): MutableMetaDelegate<Meta?> = object : MutableMetaDelegate<Meta?> {

    override val descriptor: MetaDescriptor? = descriptor

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
): MutableMetaDelegate<T?> = object : MutableMetaDelegate<T?> {

    override val descriptor: MetaDescriptor? get() = converter.descriptor


    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: property.name.asName()
        return get(name)?.let { converter.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: property.name.asName()
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
        val name = key ?: property.name.asName()
        return get(name)?.let { converter.read(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
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
        val name = key ?: property.name.asName()
        return getIndexedList(name).map { converter.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        val name = key ?: property.name.asName()
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
        get(key ?: property.name.asName())?.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Value?) {
        setValue(key ?: property.name.asName(), value)
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
        reader(get(key ?: property.name.asName())?.value)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setValue(key ?: property.name.asName(), writer(value))
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
): MutableMetaDelegate<E> = value(key) { value -> value?.string?.let { enumValueOf<E>(it) } ?: default }

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
 * A read-write delegate for a [ValuedEnumValue] property.
 *
 * @param E The enum type, which must implement [ValuedEnum].
 * @param T The type of the enum's value.
 * @param valueReader A function to read a value of type `T` from a `Meta` object (e.g., `Meta::int`).
 * @param valueWriter A function to write a value of type `T` into a [Value].
 * @param entryProvider A function to resolve a raw value of type `T` to an enum entry.
 * @param key The explicit [Name] of the property. If null, the name of the delegated property is used.
 */
public fun <E, T> MutableMetaProvider.valuedEnum(
    valueReader: (Meta) -> T?,
    valueWriter: (T) -> Value,
    entryProvider: (T) -> E?,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, T>?> where E : Enum<E>, E : ValuedEnum<E, T> =
    convertable(MetaConverter.valuedEnum(valueReader, valueWriter, entryProvider), key)


/**
 * A non-nullable read-write delegate for a [ValuedEnumValue] property with a default value.
 * @param default The default enum entry to use if the value is not present.
 */
public fun <E, T> MutableMetaProvider.valuedEnum(
    valueReader: (Meta) -> T?,
    valueWriter: (T) -> Value,
    entryProvider: (T) -> E?,
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, T>> where E : Enum<E>, E : ValuedEnum<E, T> =
    convertable(MetaConverter.valuedEnum(valueReader, valueWriter, entryProvider), ValuedEnumValue.of(default), key)

public fun <E> MutableMetaProvider.intValuedEnum(
    entryProvider: (Int) -> E?,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, Int>?> where E : Enum<E>, E : ValuedEnum<E, Int> =
    valuedEnum(Meta::int, Int::asValue, entryProvider, key)

public fun <E> MutableMetaProvider.intValuedEnum(
    entryProvider: (Int) -> E?,
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, Int>> where E : Enum<E>, E : ValuedEnum<E, Int> =
    valuedEnum(Meta::int, Int::asValue, entryProvider, default, key)

public fun <E> MutableMetaProvider.shortValuedEnum(
    entryProvider: (Short) -> E?,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, Short>?> where E : Enum<E>, E : ValuedEnum<E, Short> =
    valuedEnum(Meta::short, Short::asValue, entryProvider, key)

public fun <E> MutableMetaProvider.shortValuedEnum(
    entryProvider: (Short) -> E?,
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, Short>> where E : Enum<E>, E : ValuedEnum<E, Short> =
    valuedEnum(Meta::short, Short::asValue, entryProvider, default, key)

public fun <E> MutableMetaProvider.byteValuedEnum(
    entryProvider: (Byte) -> E?,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, Byte>?> where E : Enum<E>, E : ValuedEnum<E, Byte> =
    valuedEnum({ it.number?.toByte() }, Byte::asValue, entryProvider, key)

public fun <E> MutableMetaProvider.byteValuedEnum(
    entryProvider: (Byte) -> E?,
    default: E,
    key: Name? = null,
): ReadWriteProperty<Any?, ValuedEnumValue<E, Byte>> where E : Enum<E>, E : ValuedEnum<E, Byte> =
    valuedEnum({ it.number?.toByte() }, Byte::asValue, entryProvider, default, key)