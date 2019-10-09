package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.values.DoubleArrayValue
import hep.dataforge.values.Null
import hep.dataforge.values.Value
import kotlin.jvm.JvmName


//Configurable delegates

/**
 * A property delegate that uses custom key
 */
fun Configurable.value(default: Any = Null, key: Name? = null): MutableValueDelegate<Config> =
    MutableValueDelegate(config, key, Value.of(default))

fun <T> Configurable.value(
    default: T? = null,
    key: Name? = null,
    writer: (T) -> Value = { Value.of(it) },
    reader: (Value?) -> T
): ReadWriteDelegateWrapper<Value?, T> =
    MutableValueDelegate(config, key, default?.let { Value.of(it) }).transform(reader = reader, writer = writer)

fun Configurable.string(default: String? = null, key: Name? = null): MutableStringDelegate<Config> =
    MutableStringDelegate(config, key, default)

fun Configurable.boolean(default: Boolean? = null, key: Name? = null): MutableBooleanDelegate<Config> =
    MutableBooleanDelegate(config, key, default)

fun Configurable.number(default: Number? = null, key: Name? = null): MutableNumberDelegate<Config> =
    MutableNumberDelegate(config, key, default)

/* Number delegates*/

fun Configurable.int(default: Int? = null, key: Name? = null) =
    number(default, key).int

fun Configurable.double(default: Double? = null, key: Name? = null) =
    number(default, key).double

fun Configurable.long(default: Long? = null, key: Name? = null) =
    number(default, key).long

fun Configurable.short(default: Short? = null, key: Name? = null) =
    number(default, key).short

fun Configurable.float(default: Float? = null, key: Name? = null) =
    number(default, key).float


@JvmName("safeString")
fun Configurable.string(default: String, key: Name? = null) =
    MutableSafeStringDelegate(config, key) { default }

@JvmName("safeBoolean")
fun Configurable.boolean(default: Boolean, key: Name? = null) =
    MutableSafeBooleanDelegate(config, key) { default }

@JvmName("safeNumber")
fun Configurable.number(default: Number, key: Name? = null) =
    MutableSafeNumberDelegate(config, key) { default }

@JvmName("safeString")
fun Configurable.string(key: Name? = null, default: () -> String) =
    MutableSafeStringDelegate(config, key, default)

@JvmName("safeBoolean")
fun Configurable.boolean(key: Name? = null, default: () -> Boolean) =
    MutableSafeBooleanDelegate(config, key, default)

@JvmName("safeNumber")
fun Configurable.number(key: Name? = null, default: () -> Number) =
    MutableSafeNumberDelegate(config, key, default)


/* Safe number delegates*/

@JvmName("safeInt")
fun Configurable.int(default: Int, key: Name? = null) =
    number(default, key).int

@JvmName("safeDouble")
fun Configurable.double(default: Double, key: Name? = null) =
    number(default, key).double

@JvmName("safeLong")
fun Configurable.long(default: Long, key: Name? = null) =
    number(default, key).long

@JvmName("safeShort")
fun Configurable.short(default: Short, key: Name? = null) =
    number(default, key).short

@JvmName("safeFloat")
fun Configurable.float(default: Float, key: Name? = null) =
    number(default, key).float

/**
 * Enum delegate
 */
inline fun <reified E : Enum<E>> Configurable.enum(default: E, key: Name? = null) =
    MutableSafeEnumvDelegate(config, key, default) { enumValueOf(it) }

/* Node delegates */

fun Configurable.node(key: Name? = null): MutableNodeDelegate<Config> = MutableNodeDelegate(config, key)

fun <T : Specific> Configurable.spec(spec: Specification<T>, key: Name? = null) =
    MutableMorphDelegate(config, key) { spec.wrap(it) }

fun <T : Specific> Configurable.spec(builder: (Config) -> T, key: Name? = null) =
    MutableMorphDelegate(config, key) { specification(builder).wrap(it) }

/*
 * Extra delegates for special cases
 */

fun Configurable.stringList(key: Name? = null): ReadWriteDelegateWrapper<Value?, List<String>> =
    value(emptyList(), key) { it?.list?.map { value -> value.string } ?: emptyList() }

fun Configurable.numberList(key: Name? = null): ReadWriteDelegateWrapper<Value?, List<Number>> =
    value(emptyList(), key) { it?.list?.map { value -> value.number } ?: emptyList() }

/**
 * A special delegate for double arrays
 */
fun Configurable.doubleArray(key: Name? = null): ReadWriteDelegateWrapper<Value?, DoubleArray> =
    value(doubleArrayOf(), key) {
        (it as? DoubleArrayValue)?.value
            ?: it?.list?.map { value -> value.number.toDouble() }?.toDoubleArray()
            ?: doubleArrayOf()
    }

fun <T : Configurable> Configurable.node(key: Name? = null, converter: (Meta) -> T) =
    MutableMorphDelegate(config, key, converter)
