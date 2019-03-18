package hep.dataforge.meta

import hep.dataforge.values.Null
import hep.dataforge.values.Value
import hep.dataforge.values.asValue
import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Meta delegates */

//TODO add caching for sealed nodes

class ValueDelegate(val meta: Meta, private val key: String? = null, private val default: Value? = null) :
    ReadOnlyProperty<Any?, Value?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Value? {
        return meta[key ?: property.name]?.value ?: default
    }
}

class StringDelegate(val meta: Meta, private val key: String? = null, private val default: String? = null) :
    ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return meta[key ?: property.name]?.string ?: default
    }
}

class BooleanDelegate(val meta: Meta, private val key: String? = null, private val default: Boolean? = null) :
    ReadOnlyProperty<Metoid, Boolean?> {
    override fun getValue(thisRef: Metoid, property: KProperty<*>): Boolean? {
        return meta[key ?: property.name]?.boolean ?: default
    }
}

class NumberDelegate(val meta: Meta, private val key: String? = null, private val default: Number? = null) :
    ReadOnlyProperty<Any?, Number?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Number? {
        return meta[key ?: property.name]?.number ?: default
    }

    //delegates for number transformation

    val double get() = DelegateWrapper(this) { it?.toDouble() }
    val int get() = DelegateWrapper(this) { it?.toInt() }
    val short get() = DelegateWrapper(this) { it?.toShort() }
    val long get() = DelegateWrapper(this) { it?.toLong() }
}

class DelegateWrapper<T, R>(val delegate: ReadOnlyProperty<Any?, T>, val reader: (T) -> R) :
    ReadOnlyProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R {
        return reader(delegate.getValue(thisRef, property))
    }
}

//Delegates with non-null values

class SafeStringDelegate(
    val meta: Meta,
    private val key: String? = null,
    default: () -> String
) : ReadOnlyProperty<Any?, String> {

    private val default: String by lazy(default)

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return meta[key ?: property.name]?.string ?: default
    }
}

class SafeBooleanDelegate(
    val meta: Meta,
    private val key: String? = null,
    default: () -> Boolean
) : ReadOnlyProperty<Any?, Boolean> {

    private val default: Boolean by lazy(default)

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return meta[key ?: property.name]?.boolean ?: default
    }
}

class SafeNumberDelegate(
    val meta: Meta,
    private val key: String? = null,
    default: () -> Number
) : ReadOnlyProperty<Any?, Number> {

    private val default: Number by lazy(default)

    override fun getValue(thisRef: Any?, property: KProperty<*>): Number {
        return meta[key ?: property.name]?.number ?: default
    }

    val double get() = DelegateWrapper(this) { it.toDouble() }
    val int get() = DelegateWrapper(this) { it.toInt() }
    val short get() = DelegateWrapper(this) { it.toShort() }
    val long get() = DelegateWrapper(this) { it.toLong() }
}

class SafeEnumDelegate<E : Enum<E>>(
    val meta: Meta,
    private val key: String? = null,
    private val default: E,
    private val resolver: (String) -> E
) : ReadOnlyProperty<Any?, E> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): E {
        return (meta[key ?: property.name]?.string)?.let { resolver(it) } ?: default
    }
}

//Child node delegate

class ChildDelegate<T>(val meta: Meta, private val key: String? = null, private val converter: (Meta) -> T) :
    ReadOnlyProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return meta[key ?: property.name]?.node?.let { converter(it) }
    }
}

//Read-only delegates for Metas

/**
 * A property delegate that uses custom key
 */
fun Meta.value(default: Value = Null, key: String? = null) = ValueDelegate(this, key, default)

fun Meta.string(default: String? = null, key: String? = null) = StringDelegate(this, key, default)

fun Meta.boolean(default: Boolean? = null, key: String? = null) = BooleanDelegate(this, key, default)

fun Meta.number(default: Number? = null, key: String? = null) = NumberDelegate(this, key, default)

fun Meta.child(key: String? = null) = ChildDelegate(this, key) { it }

@JvmName("safeString")
fun Meta.string(default: String, key: String? = null) =
    SafeStringDelegate(this, key) { default }

@JvmName("safeBoolean")
fun Meta.boolean(default: Boolean, key: String? = null) =
    SafeBooleanDelegate(this, key) { default }

@JvmName("safeNumber")
fun Meta.number(default: Number, key: String? = null) =
    SafeNumberDelegate(this, key) { default }

@JvmName("safeString")
fun Meta.string(key: String? = null, default: () -> String) =
    SafeStringDelegate(this, key, default)

@JvmName("safeBoolean")
fun Meta.boolean(key: String? = null, default: () -> Boolean) =
    SafeBooleanDelegate(this, key, default)

@JvmName("safeNumber")
fun Meta.number(key: String? = null, default: () -> Number) =
    SafeNumberDelegate(this, key, default)


inline fun <reified E : Enum<E>> Meta.enum(default: E, key: String? = null) =
    SafeEnumDelegate(this, key, default) { enumValueOf(it) }


/* Config delegates */

class ValueConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    private val default: Value? = null
) : ReadWriteProperty<Any?, Value?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Value? {
        return config[key ?: property.name]?.value ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Value?) {
        val name = key ?: property.name
        if (value == null) {
            config.remove(name)
        } else {
            config.setValue(name, value)
        }
    }

    fun <T> map(writer: (T) -> Value? = { Value.of(it) }, reader: (Value?) -> T) =
        ReadWriteDelegateWrapper(this, reader, writer)
}

class StringConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    private val default: String? = null
) : ReadWriteProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return config[key ?: property.name]?.string ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        val name = key ?: property.name
        if (value == null) {
            config.remove(name)
        } else {
            config.setValue(name, value.asValue())
        }
    }
}

class BooleanConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    private val default: Boolean? = null
) : ReadWriteProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
        return config[key ?: property.name]?.boolean ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
        val name = key ?: property.name
        if (value == null) {
            config.remove(name)
        } else {
            config.setValue(name, value.asValue())
        }
    }
}

class NumberConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    private val default: Number? = null
) : ReadWriteProperty<Any?, Number?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Number? {
        return config[key ?: property.name]?.number ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Number?) {
        val name = key ?: property.name
        if (value == null) {
            config.remove(name)
        } else {
            config.setValue(name, value.asValue())
        }
    }

    val double get() = ReadWriteDelegateWrapper(this, reader = { it?.toDouble() }, writer = { it })
    val int get() = ReadWriteDelegateWrapper(this, reader = { it?.toInt() }, writer = { it })
    val short get() = ReadWriteDelegateWrapper(this, reader = { it?.toShort() }, writer = { it })
    val long get() = ReadWriteDelegateWrapper(this, reader = { it?.toLong() }, writer = { it })
}

//Delegates with non-null values

class SafeStringConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    default: () -> String
) : ReadWriteProperty<Any?, String> {

    private val default: String by lazy(default)

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return config[key ?: property.name]?.string ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        config.setValue(key ?: property.name, value.asValue())
    }
}

class SafeBooleanConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    default: () -> Boolean
) : ReadWriteProperty<Any?, Boolean> {

    private val default: Boolean by lazy(default)

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return config[key ?: property.name]?.boolean ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        config.setValue(key ?: property.name, value.asValue())
    }
}

class SafeNumberConfigDelegate<M : MutableMeta<M>>(
    val config: M,
    private val key: String? = null,
    default: () -> Number
) : ReadWriteProperty<Any?, Number> {

    private val default: Number by lazy(default)

    override fun getValue(thisRef: Any?, property: KProperty<*>): Number {
        return config[key ?: property.name]?.number ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Number) {
        config.setValue(key ?: property.name, value.asValue())
    }

    val double get() = ReadWriteDelegateWrapper(this, reader = { it.toDouble() }, writer = { it })
    val int get() = ReadWriteDelegateWrapper(this, reader = { it.toInt() }, writer = { it })
    val short get() = ReadWriteDelegateWrapper(this, reader = { it.toShort() }, writer = { it })
    val long get() = ReadWriteDelegateWrapper(this, reader = { it.toLong() }, writer = { it })
}

class SafeEnumvConfigDelegate<M : MutableMeta<M>, E : Enum<E>>(
    val config: M,
    private val key: String? = null,
    private val default: E,
    private val resolver: (String) -> E
) : ReadWriteProperty<Any?, E> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): E {
        return (config[key ?: property.name]?.string)?.let { resolver(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: E) {
        config.setValue(key ?: property.name, value.name.asValue())
    }
}

//Child node delegate

class MetaNodeDelegate<M : MutableMetaNode<M>>(
    val config: M,
    private val key: String? = null
) : ReadWriteProperty<Any?, Meta> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Meta {
        return config[key ?: property.name]?.node ?: EmptyMeta
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Meta) {
        config[key ?: property.name] = value
    }
}

class ChildConfigDelegate<M : MutableMetaNode<M>, T : Configurable>(
    val config: M,
    private val key: String? = null,
    private val converter: (Meta) -> T
) :
    ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return converter(config[key ?: property.name]?.node ?: EmptyMeta)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        config[key ?: property.name] = value.config
    }
}

class ReadWriteDelegateWrapper<T, R>(
    val delegate: ReadWriteProperty<Any?, T>,
    val reader: (T) -> R,
    val writer: (R) -> T
) : ReadWriteProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R {
        return reader(delegate.getValue(thisRef, property))
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        delegate.setValue(thisRef, property, writer(value))
    }
}


//Read-write delegates

/**
 * A property delegate that uses custom key
 */
fun <M : MutableMeta<M>> M.value(default: Value = Null, key: String? = null) =
    ValueConfigDelegate(this, key, default)

fun <M : MutableMeta<M>> M.string(default: String? = null, key: String? = null) =
    StringConfigDelegate(this, key, default)

fun <M : MutableMeta<M>> M.boolean(default: Boolean? = null, key: String? = null) =
    BooleanConfigDelegate(this, key, default)

fun <M : MutableMeta<M>> M.number(default: Number? = null, key: String? = null) =
    NumberConfigDelegate(this, key, default)

fun <M : MutableMetaNode<M>> M.child(key: String? = null) = MetaNodeDelegate(this, key)

//fun <T : Configurable> Configurable.spec(spec: Specification<T>, key: String? = null) = ChildConfigDelegate<T>(key) { spec.wrap(this) }

@JvmName("safeString")
fun <M : MutableMeta<M>> M.string(default: String, key: String? = null) =
    SafeStringConfigDelegate(this, key) { default }

@JvmName("safeBoolean")
fun <M : MutableMeta<M>> M.boolean(default: Boolean, key: String? = null) =
    SafeBooleanConfigDelegate(this, key) { default }

@JvmName("safeNumber")
fun <M : MutableMeta<M>> M.number(default: Number, key: String? = null) =
    SafeNumberConfigDelegate(this, key) { default }

@JvmName("safeString")
fun <M : MutableMeta<M>> M.string(key: String? = null, default: () -> String) =
    SafeStringConfigDelegate(this, key, default)

@JvmName("safeBoolean")
fun <M : MutableMeta<M>> M.boolean(key: String? = null, default: () -> Boolean) =
    SafeBooleanConfigDelegate(this, key, default)

@JvmName("safeNumber")
fun <M : MutableMeta<M>> M.number(key: String? = null, default: () -> Number) =
    SafeNumberConfigDelegate(this, key, default)


inline fun <M : MutableMeta<M>, reified E : Enum<E>> M.enum(default: E, key: String? = null) =
    SafeEnumvConfigDelegate(this, key, default) { enumValueOf(it) }
