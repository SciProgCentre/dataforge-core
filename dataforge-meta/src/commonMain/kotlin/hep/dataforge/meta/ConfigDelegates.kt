package hep.dataforge.meta

import hep.dataforge.values.Null
import hep.dataforge.values.Value
import kotlin.jvm.JvmName


//Configurable delegates

/**
 * A property delegate that uses custom key
 */
fun Configurable.value(default: Any = Null, key: String? = null) =
    MutableValueDelegate(config, key, Value.of(default))

fun Configurable.string(default: String? = null, key: String? = null) =
    MutableStringDelegate(config, key, default)

fun Configurable.boolean(default: Boolean? = null, key: String? = null) =
    MutableBooleanDelegate(config, key, default)

fun Configurable.number(default: Number? = null, key: String? = null) =
    MutableNumberDelegate(config, key, default)

fun Configurable.node(key: String? = null) = MutableNodeDelegate(config, key)

//fun <T : Configurable> Configurable.spec(spec: Specification<T>, key: String? = null) = ChildConfigDelegate<T>(key) { spec.wrap(this) }

@JvmName("safeString")
fun Configurable.string(default: String, key: String? = null) =
    MutableSafeStringDelegate(config, key) { default }

@JvmName("safeBoolean")
fun Configurable.boolean(default: Boolean, key: String? = null) =
    MutableSafeBooleanDelegate(config, key) { default }

@JvmName("safeNumber")
fun Configurable.number(default: Number, key: String? = null) =
    MutableSafeNumberDelegate(config, key) { default }

@JvmName("safeString")
fun Configurable.string(key: String? = null, default: () -> String) =
    MutableSafeStringDelegate(config, key, default)

@JvmName("safeBoolean")
fun Configurable.boolean(key: String? = null, default: () -> Boolean) =
    MutableSafeBooleanDelegate(config, key, default)

@JvmName("safeNumber")
fun Configurable.number(key: String? = null, default: () -> Number) =
    MutableSafeNumberDelegate(config, key, default)


inline fun <reified E : Enum<E>> Configurable.enum(default: E, key: String? = null) =
    MutableSafeEnumvDelegate(config, key, default) { enumValueOf(it) }