package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.*
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


//delegates

/**
 * A delegate that uses a [Configurable] object and delegate read and write operations to its properties
 */
open class ConfigurableDelegate(
    val owner: Configurable,
    val key: Name? = null,
    open val default: MetaItem<*>? = null
) : ReadWriteProperty<Any?, MetaItem<*>?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): MetaItem<*>? {
        val name = key ?: property.name.asName()
        return owner.getProperty(name) ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MetaItem<*>?) {
        val name = key ?: property.name.asName()
        owner.setProperty(name, value)
    }
}

class LazyConfigurableDelegate(
    configurable: Configurable,
    key: Name? = null,
    defaultProvider: () -> MetaItem<*>? = { null }
) : ConfigurableDelegate(configurable, key) {
    override val default by lazy(defaultProvider)
}

/**
 * A property delegate that uses custom key
 */
fun Configurable.item(default: Any? = null, key: Name? = null): ConfigurableDelegate =
    ConfigurableDelegate(
        this,
        key,
        default?.let { MetaItem.of(it) })

/**
 * Generation of item delegate with lazy default.
 * Lazy default could be used also for validation
 */
fun Configurable.lazyItem(key: Name? = null, default: () -> Any?): ConfigurableDelegate =
    LazyConfigurableDelegate(this, key) {
        default()?.let {
            MetaItem.of(it)
        }
    }

fun <T> Configurable.item(
    default: T? = null,
    key: Name? = null,
    writer: (T) -> MetaItem<*>? = {
        MetaItem.of(it)
    },
    reader: (MetaItem<*>?) -> T
): ReadWriteProperty<Any?, T> =
    ConfigurableDelegate(
        this,
        key,
        default?.let { MetaItem.of(it) }).map(reader = reader, writer = writer)

fun Configurable.value(default: Any? = null, key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(default, key).transform { it.value }

fun <T> Configurable.value(
    default: T? = null,
    key: Name? = null,
    writer: (T) -> Value? = { Value.of(it) },
    reader: (Value?) -> T
): ReadWriteProperty<Any?, T> =
    ConfigurableDelegate(
        this,
        key,
        default?.let { MetaItem.of(it) }
    ).map(
        reader = { reader(it.value) },
        writer = { value -> writer(value)?.let { MetaItem.ValueItem(it) } }
    )

fun Configurable.string(default: String? = null, key: Name? = null): ReadWriteProperty<Any?, String?> =
    item(default, key).transform { it.value?.string }

fun Configurable.boolean(default: Boolean? = null, key: Name? = null): ReadWriteProperty<Any?, Boolean?> =
    item(default, key).transform { it.value?.boolean }

fun Configurable.number(default: Number? = null, key: Name? = null): ReadWriteProperty<Any?, Number?> =
    item(default, key).transform { it.value?.number }

/* Number delegates*/

fun Configurable.int(default: Int? = null, key: Name? = null): ReadWriteProperty<Any?, Int?> =
    item(default, key).transform { it.value?.int }

fun Configurable.double(default: Double? = null, key: Name? = null): ReadWriteProperty<Any?, Double?> =
    item(default, key).transform { it.value?.double }

fun Configurable.long(default: Long? = null, key: Name? = null): ReadWriteProperty<Any?, Long?> =
    item(default, key).transform { it.value?.long }

fun Configurable.short(default: Short? = null, key: Name? = null): ReadWriteProperty<Any?, Short?> =
    item(default, key).transform { it.value?.short }

fun Configurable.float(default: Float? = null, key: Name? = null): ReadWriteProperty<Any?, Float?> =
    item(default, key).transform { it.value?.float }


@JvmName("safeString")
fun Configurable.string(default: String, key: Name? = null): ReadWriteProperty<Any?, String> =
    item(default, key).transform { it.value!!.string }

@JvmName("safeBoolean")
fun Configurable.boolean(default: Boolean, key: Name? = null): ReadWriteProperty<Any?, Boolean> =
    item(default, key).transform { it.value!!.boolean }

@JvmName("safeNumber")
fun Configurable.number(default: Number, key: Name? = null): ReadWriteProperty<Any?, Number> =
    item(default, key).transform { it.value!!.number }

/* Lazy initializers for values */

@JvmName("lazyString")
fun Configurable.string(key: Name? = null, default: () -> String): ReadWriteProperty<Any?, String> =
    lazyItem(key, default).transform { it.value!!.string }

@JvmName("lazyBoolean")
fun Configurable.boolean(key: Name? = null, default: () -> Boolean): ReadWriteProperty<Any?, Boolean> =
    lazyItem(key, default).transform { it.value!!.boolean }

@JvmName("lazyNumber")
fun Configurable.number(key: Name? = null, default: () -> Number): ReadWriteProperty<Any?, Number> =
    lazyItem(key, default).transform { it.value!!.number }

/* Safe number delegates*/

@JvmName("safeInt")
fun Configurable.int(default: Int, key: Name? = null): ReadWriteProperty<Any?, Int> =
    item(default, key).transform { it.value!!.int }

@JvmName("safeDouble")
fun Configurable.double(default: Double, key: Name? = null): ReadWriteProperty<Any?, Double> =
    item(default, key).transform { it.value!!.double }

@JvmName("safeLong")
fun Configurable.long(default: Long, key: Name? = null): ReadWriteProperty<Any?, Long> =
    item(default, key).transform { it.value!!.long }

@JvmName("safeShort")
fun Configurable.short(default: Short, key: Name? = null): ReadWriteProperty<Any?, Short> =
    item(default, key).transform { it.value!!.short }

@JvmName("safeFloat")
fun Configurable.float(default: Float, key: Name? = null): ReadWriteProperty<Any?, Float> =
    item(default, key).transform { it.value!!.float }

/**
 * Enum delegate
 */
inline fun <reified E : Enum<E>> Configurable.enum(
    default: E, key: Name? = null
): ReadWriteProperty<Any?, E> =
    item(default, key).transform { item -> item?.string?.let {str->
        @Suppress("USELESS_CAST")
        enumValueOf<E>(str)  as E
    } ?: default }

/*
 * Extra delegates for special cases
 */
fun Configurable.stringList(vararg strings: String, key: Name? = null): ReadWriteProperty<Any?, List<String>> =
    item(listOf(*strings), key) {
        it?.value?.stringList ?: emptyList()
    }

fun Configurable.numberList(vararg numbers: Number, key: Name? = null): ReadWriteProperty<Any?, List<Number>> =
    item(listOf(*numbers), key) { item ->
        item?.value?.list?.map { it.number } ?: emptyList()
    }

/**
 * A special delegate for double arrays
 */
fun Configurable.doubleArray(vararg doubles: Double, key: Name? = null): ReadWriteProperty<Any?, DoubleArray> =
    item(doubleArrayOf(*doubles), key) {
        (it.value as? DoubleArrayValue)?.value
            ?: it?.value?.list?.map { value -> value.number.toDouble() }?.toDoubleArray()
            ?: doubleArrayOf()
    }


/* Node delegates */

fun Configurable.config(default: Config? = null, key: Name? = null): ReadWriteProperty<Any?, Config?> =
    config.node(default,key)

fun Configurable.node(key: Name? = null): ReadWriteProperty<Any?, Meta?> = item(key).map(
    reader = { it.node },
    writer = { it?.let { MetaItem.NodeItem(it) } }
)

fun <T : Configurable> Configurable.spec(
    spec: Specification<T>, key: Name? = null
): ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: property.name.asName()
        return config[name].node?.let { spec.wrap(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: property.name.asName()
        config[name] = value?.config
    }
}

fun <T : Configurable> Configurable.spec(
    spec: Specification<T>, default: T, key: Name? = null
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return config[name].node?.let { spec.wrap(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        config[name] = value.config
    }
}

