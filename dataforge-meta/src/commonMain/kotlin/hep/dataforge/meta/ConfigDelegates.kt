package hep.dataforge.meta

import hep.dataforge.values.Null
import hep.dataforge.values.Value
import kotlin.jvm.JvmName


//Configurable delegates

/**
 * A property delegate that uses custom key
 */
fun Configurable.value(default: Value = Null, key: String? = null) =
    ValueConfigDelegate(config, key, default)

fun Configurable.string(default: String? = null, key: String? = null) =
    StringConfigDelegate(config, key, default)

fun Configurable.boolean(default: Boolean? = null, key: String? = null) =
    BooleanConfigDelegate(config, key, default)

fun Configurable.number(default: Number? = null, key: String? = null) =
    NumberConfigDelegate(config, key, default)

fun Configurable.child(key: String? = null) = MetaNodeDelegate(config, key)

//fun <T : Configurable> Configurable.spec(spec: Specification<T>, key: String? = null) = ChildConfigDelegate<T>(key) { spec.wrap(this) }

@JvmName("safeString")
fun Configurable.string(default: String, key: String? = null) =
    SafeStringConfigDelegate(config, key, default)

@JvmName("safeBoolean")
fun Configurable.boolean(default: Boolean, key: String? = null) =
    SafeBooleanConfigDelegate(config, key, default)

@JvmName("safeNumber")
fun Configurable.number(default: Number, key: String? = null) =
    SafeNumberConfigDelegate(config, key, default)

inline fun <reified E : Enum<E>> Configurable.enum(default: E, key: String? = null) =
    SafeEnumvConfigDelegate(config, key, default) { enumValueOf(it) }