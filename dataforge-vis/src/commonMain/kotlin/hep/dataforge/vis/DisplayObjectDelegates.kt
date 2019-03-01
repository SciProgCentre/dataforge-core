package hep.dataforge.vis

import hep.dataforge.meta.*
import hep.dataforge.values.Null
import hep.dataforge.values.Value
import kotlin.jvm.JvmName

fun DisplayObject.value(default: Value = Null, key: String? = null) =
    ValueConfigDelegate(properties, key, default)

fun DisplayObject.string(default: String? = null, key: String? = null) =
    StringConfigDelegate(properties, key, default)

fun DisplayObject.boolean(default: Boolean? = null, key: String? = null) =
    BooleanConfigDelegate(properties, key, default)

fun DisplayObject.number(default: Number? = null, key: String? = null) =
    NumberConfigDelegate(properties, key, default)

fun DisplayObject.double(default: Double? = null, key: String? = null) =
    NumberConfigDelegate(properties, key, default).double

fun DisplayObject.int(default: Int? = null, key: String? = null) =
    NumberConfigDelegate(properties, key, default).int


fun DisplayObject.node(key: String? = null) = StyledNodeDelegate(properties, key)

//fun <T : Configurable> Configurable.spec(spec: Specification<T>, key: String? = null) = ChildConfigDelegate<T>(key) { spec.wrap(this) }

@JvmName("safeString")
fun DisplayObject.string(default: String, key: String? = null) =
    SafeStringConfigDelegate(properties, key, default)

@JvmName("safeBoolean")
fun DisplayObject.boolean(default: Boolean, key: String? = null) =
    SafeBooleanConfigDelegate(properties, key, default)

@JvmName("safeNumber")
fun DisplayObject.number(default: Number, key: String? = null) =
    SafeNumberConfigDelegate(properties, key, default)

@JvmName("safeDouble")
fun DisplayObject.double(default: Double, key: String? = null) =
    SafeNumberConfigDelegate(properties, key, default).double

inline fun <reified E : Enum<E>> DisplayObject.enum(default: E, key: String? = null) =
    SafeEnumvConfigDelegate(properties, key, default) { enumValueOf(it) }