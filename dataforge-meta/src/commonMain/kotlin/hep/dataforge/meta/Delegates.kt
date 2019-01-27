package hep.dataforge.meta

import hep.dataforge.values.Null
import hep.dataforge.values.Value
import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Meta delegates */

//TODO add caching for sealed nodes

class ValueDelegate(private val key: String? = null, private val default: Value? = null) :
    ReadOnlyProperty<Metoid, Value?> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): Value? {
        return thisRef.meta[key ?: property.name]?.value ?: default
    }
}

class StringDelegate(private val key: String? = null, private val default: String? = null) :
    ReadOnlyProperty<Metoid, String?> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): String? {
        return thisRef.meta[key ?: property.name]?.string ?: default
    }
}

class BooleanDelegate(private val key: String? = null, private val default: Boolean? = null) :
    ReadOnlyProperty<Metoid, Boolean?> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): Boolean? {
        return thisRef.meta[key ?: property.name]?.boolean ?: default
    }
}

class NumberDelegate(private val key: String? = null, private val default: Number? = null) :
    ReadOnlyProperty<Metoid, Number?> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): Number? {
        return thisRef.meta[key ?: property.name]?.number ?: default
    }
}

//Delegates with non-null values

class SafeStringDelegate(private val key: String? = null, private val default: String) :
    ReadOnlyProperty<Metoid, String> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): String {
        return thisRef.meta[key ?: property.name]?.string ?: default
    }
}

class SafeBooleanDelegate(private val key: String? = null, private val default: Boolean) :
    ReadOnlyProperty<Metoid, Boolean> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): Boolean {
        return thisRef.meta[key ?: property.name]?.boolean ?: default
    }
}

class SafeNumberDelegate(private val key: String? = null, private val default: Number) :
    ReadOnlyProperty<Metoid, Number> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): Number {
        return thisRef.meta[key ?: property.name]?.number ?: default
    }
}

class SafeEnumDelegate<E : Enum<E>>(
    private val key: String? = null,
    private val default: E,
    private val resolver: (String) -> E
) : ReadOnlyProperty<Metoid, E> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): E {
        return (thisRef.meta[key ?: property.name]?.string)?.let { resolver(it) } ?: default
    }
}

//Child node delegate

class ChildDelegate<T>(private val key: String? = null, private val converter: (Meta) -> T) :
    ReadOnlyProperty<Metoid, T?> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): T? {
        return thisRef.meta[key ?: property.name]?.node?.let { converter(it) }
    }
}

//Read-only delegates

/**
 * A property delegate that uses custom key
 */
fun Metoid.value(default: Value = Null, key: String? = null) = ValueDelegate(key, default)

fun Metoid.string(default: String? = null, key: String? = null) = StringDelegate(key, default)

fun Metoid.boolean(default: Boolean? = null, key: String? = null) = BooleanDelegate(key, default)

fun Metoid.number(default: Number? = null, key: String? = null) = NumberDelegate(key, default)

fun Metoid.child(key: String? = null) = ChildDelegate(key) { it }

fun <T : Metoid> Metoid.child(key: String? = null, converter: (Meta) -> T) = ChildDelegate(key, converter)

@JvmName("safeString")
fun Metoid.string(default: String, key: String? = null) = SafeStringDelegate(key, default)

@JvmName("safeBoolean")
fun Metoid.boolean(default: Boolean, key: String? = null) = SafeBooleanDelegate(key, default)

@JvmName("safeNumber")
fun Metoid.number(default: Number, key: String? = null) = SafeNumberDelegate(key, default)

inline fun <reified E : Enum<E>> Metoid.enum(default: E, key: String? = null) =
    SafeEnumDelegate(key, default) { enumValueOf(it) }

/* Config delegates */

class ValueConfigDelegate(private val key: String? = null, private val default: Value? = null) :
    ReadWriteProperty<Configurable, Value?> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): Value? {
        return thisRef.config[key ?: property.name]?.value ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: Value?) {
        val name = key ?: property.name
        if (value == null) {
            thisRef.config.remove(name)
        } else {
            thisRef.config[name] = value
        }
    }
}

class StringConfigDelegate(private val key: String? = null, private val default: String? = null) :
    ReadWriteProperty<Configurable, String?> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): String? {
        return thisRef.config[key ?: property.name]?.string ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: String?) {
        thisRef.config[key ?: property.name] = value
    }
}

class BooleanConfigDelegate(private val key: String? = null, private val default: Boolean? = null) :
    ReadWriteProperty<Configurable, Boolean?> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): Boolean? {
        return thisRef.config[key ?: property.name]?.boolean ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: Boolean?) {
        thisRef.config[key ?: property.name] = value
    }
}

class NumberConfigDelegate(private val key: String? = null, private val default: Number? = null) :
    ReadWriteProperty<Configurable, Number?> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): Number? {
        return thisRef.config[key ?: property.name]?.number ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: Number?) {
        thisRef.config[key ?: property.name] = value
    }
}

//Delegates with non-null values

class SafeStringConfigDelegate(private val key: String? = null, private val default: String) :
    ReadWriteProperty<Configurable, String> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): String {
        return thisRef.config[key ?: property.name]?.string ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: String) {
        thisRef.config[key ?: property.name] = value
    }
}

class SafeBooleanConfigDelegate(private val key: String? = null, private val default: Boolean) :
    ReadWriteProperty<Configurable, Boolean> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): Boolean {
        return thisRef.config[key ?: property.name]?.boolean ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: Boolean) {
        thisRef.config[key ?: property.name] = value
    }
}

class SafeNumberConfigDelegate(private val key: String? = null, private val default: Number) :
    ReadWriteProperty<Configurable, Number> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): Number {
        return thisRef.config[key ?: property.name]?.number ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: Number) {
        thisRef.config[key ?: property.name] = value
    }
}

class SafeEnumvConfigDelegate<E : Enum<E>>(
    private val key: String? = null,
    private val default: E,
    private val resolver: (String) -> E
) : ReadWriteProperty<Configurable, E> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): E {
        return (thisRef.config[key ?: property.name]?.string)?.let { resolver(it) } ?: default
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: E) {
        thisRef.config[key ?: property.name] = value.name
    }
}

//Child node delegate

class ChildConfigDelegate<T : Configurable>(private val key: String? = null, private val converter: (Config) -> T) :
    ReadWriteProperty<Configurable, T> {
    override fun getValue(thisRef: Configurable, property: KProperty<*>): T {
        return converter(thisRef.config[key ?: property.name]?.node ?: Config())
    }

    override fun setValue(thisRef: Configurable, property: KProperty<*>, value: T) {
        thisRef.config[key ?: property.name] = value.config
    }

}

//Read-write delegates

/**
 * A property delegate that uses custom key
 */
fun Configurable.value(default: Value = Null, key: String? = null) = ValueConfigDelegate(key, default)

fun Configurable.string(default: String? = null, key: String? = null) = StringConfigDelegate(key, default)

fun Configurable.boolean(default: Boolean? = null, key: String? = null) = BooleanConfigDelegate(key, default)

fun Configurable.number(default: Number? = null, key: String? = null) = NumberConfigDelegate(key, default)

fun Configurable.child(key: String? = null) = ChildConfigDelegate(key) { SimpleConfigurable(it) }

fun <T : Configurable> Configurable.child(key: String? = null, converter: (Config) -> T) =
    ChildConfigDelegate(key, converter)

//fun <T : Configurable> Configurable.spec(spec: Specification<T>, key: String? = null) = ChildConfigDelegate<T>(key) { spec.wrap(this) }

@JvmName("safeString")
fun Configurable.string(default: String, key: String? = null) = SafeStringConfigDelegate(key, default)

@JvmName("safeBoolean")
fun Configurable.boolean(default: Boolean, key: String? = null) = SafeBooleanConfigDelegate(key, default)

@JvmName("safeNumber")
fun Configurable.number(default: Number, key: String? = null) = SafeNumberConfigDelegate(key, default)

inline fun <reified E : Enum<E>> Configurable.enum(default: E, key: String? = null) =
    SafeEnumvConfigDelegate(key, default) { enumValueOf(it) }