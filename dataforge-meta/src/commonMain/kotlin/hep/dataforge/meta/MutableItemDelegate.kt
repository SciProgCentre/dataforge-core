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

open class MutableItemDelegate(
    override val owner: MutableItemProvider,
    key: Name? = null,
    default: MetaItem<*>? = null
) : ItemDelegate(owner, key, default), ReadWriteProperty<Any?, MetaItem<*>?> {

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MetaItem<*>?) {
        val name = key ?: property.name.asName()
        owner.setItem(name, value)
    }
}

fun MutableItemProvider.item(key: Name? = null): MutableItemDelegate =
    MutableItemDelegate(this, key)

//Read-write delegates

/**
 * A property delegate that uses custom key
 */
fun MutableItemProvider.value(key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

fun MutableItemProvider.string(key: Name? = null): ReadWriteProperty<Any?, String?> =
    item(key).convert(MetaConverter.string)

fun MutableItemProvider.boolean(key: Name? = null): ReadWriteProperty<Any?, Boolean?> =
    item(key).convert(MetaConverter.boolean)

fun MutableItemProvider.number(key: Name? = null): ReadWriteProperty<Any?, Number?> =
    item(key).convert(MetaConverter.number)

fun MutableItemProvider.string(default: String, key: Name? = null): ReadWriteProperty<Any?, String> =
    item(key).convert(MetaConverter.string) { default }

fun MutableItemProvider.boolean(default: Boolean, key: Name? = null): ReadWriteProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean) { default }

fun MutableItemProvider.number(default: Number, key: Name? = null): ReadWriteProperty<Any?, Number> =
    item(key).convert(MetaConverter.number) { default }

fun MutableItemProvider.value(key: Name? = null, default: () -> Value): ReadWriteProperty<Any?, Value> =
    item(key).convert(MetaConverter.value, default)

fun MutableItemProvider.string(key: Name? = null, default: () -> String): ReadWriteProperty<Any?, String> =
    item(key).convert(MetaConverter.string, default)

fun MutableItemProvider.boolean(key: Name? = null, default: () -> Boolean): ReadWriteProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean, default)

fun MutableItemProvider.number(key: Name? = null, default: () -> Number): ReadWriteProperty<Any?, Number> =
    item(key).convert(MetaConverter.number, default)

inline fun <reified E : Enum<E>> MutableItemProvider.enum(default: E, key: Name? = null): ReadWriteProperty<Any?, E> =
    item(key).convert(MetaConverter.enum()) { default }

inline fun <reified M : MutableMeta<M>> M.node(key: Name? = null): ReadWriteProperty<Any?, M?> =
    item(key).convert(reader = { it?.let { it.node as M } }, writer = { it?.let { MetaItem.NodeItem(it) } })


fun Configurable.value(key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

/* Number delegates*/

fun MutableItemProvider.int(key: Name? = null): ReadWriteProperty<Any?, Int?> =
    item(key).convert(MetaConverter.int)

fun MutableItemProvider.double(key: Name? = null): ReadWriteProperty<Any?, Double?> =
    item(key).convert(MetaConverter.double)

fun MutableItemProvider.long(key: Name? = null): ReadWriteProperty<Any?, Long?> =
    item(key).convert(MetaConverter.long)

fun MutableItemProvider.float(key: Name? = null): ReadWriteProperty<Any?, Float?> =
    item(key).convert(MetaConverter.float)


/* Safe number delegates*/

fun MutableItemProvider.int(default: Int, key: Name? = null): ReadWriteProperty<Any?, Int> =
    item(key).convert(MetaConverter.int) { default }

fun MutableItemProvider.double(default: Double, key: Name? = null): ReadWriteProperty<Any?, Double> =
    item(key).convert(MetaConverter.double) { default }

fun MutableItemProvider.long(default: Long, key: Name? = null): ReadWriteProperty<Any?, Long> =
    item(key).convert(MetaConverter.long) { default }

fun MutableItemProvider.float(default: Float, key: Name? = null): ReadWriteProperty<Any?, Float> =
    item(key).convert(MetaConverter.float) { default }


/*
 * Extra delegates for special cases
 */
fun MutableItemProvider.stringList(
    vararg default: String,
    key: Name? = null
): ReadWriteProperty<Any?, List<String>> = item(key).convert(
    reader = { it?.stringList ?: listOf(*default) },
    writer = { it.map { str -> str.asValue() }.asValue().asMetaItem() }
)

fun MutableItemProvider.stringList(
    key: Name? = null
): ReadWriteProperty<Any?, List<String>?> = item(key).convert(
    reader = {  it?.stringList },
    writer = { it?.map { str -> str.asValue() }?.asValue()?.asMetaItem() }
)

fun MutableItemProvider.numberList(
    vararg default: Number,
    key: Name? = null
): ReadWriteProperty<Any?, List<Number>> = item(key).convert(
    reader = { it?.value?.list?.map { value -> value.number } ?: listOf(*default) },
    writer = { it.map { num -> num.asValue() }.asValue().asMetaItem() }
)

/**
 * A special delegate for double arrays
 */
fun MutableItemProvider.doubleArray(
    vararg default: Double,
    key: Name? = null
): ReadWriteProperty<Any?, DoubleArray> =item(key).convert(
    reader = { it?.value?.doubleArray ?: doubleArrayOf(*default) },
    writer = { DoubleArrayValue(it).asMetaItem() }
)

fun <T> MutableItemProvider.listValue(
    key: Name? = null,
    writer: (T) -> Value = { Value.of(it) },
    reader: (Value) -> T
): ReadWriteProperty<Any?, List<T>?> = item(key).convert(MetaConverter.valueList(writer, reader))
