package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.Value
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Read-write delegates */

open class MutableMetaDelegate<M : MutableMeta<M>>(
    override val owner: M,
    key: Name? = null,
    default: MetaItem<*>? = null
) : MetaDelegate(owner, key, default), ReadWriteProperty<Any?, MetaItem<*>?> {

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MetaItem<*>?) {
        val name = key ?: property.name.asName()
        owner.setItem(name, value)
    }
}

class LazyMutableMetaDelegate<M : MutableMeta<M>>(
    owner: M,
    key: Name? = null,
    defaultProvider: () -> MetaItem<*>? = { null }
) : MutableMetaDelegate<M>(owner, key) {
    override val default by lazy(defaultProvider)
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

fun <T, R> ReadWriteProperty<Any?, T>.map(reader: (T) -> R, writer: (R) -> T): ReadWriteDelegateWrapper<T, R> =
    ReadWriteDelegateWrapper(this, reader, writer)

fun <R> ReadWriteProperty<Any?, MetaItem<*>?>.transform(reader: (MetaItem<*>?) -> R): ReadWriteProperty<Any?, R> =
    map(reader = reader, writer = { MetaItem.of(it) })

fun <R> ReadWriteProperty<Any?, Value?>.transform(reader: (Value?) -> R): ReadWriteDelegateWrapper<Value?, R> =
    map(reader = reader, writer = { Value.of(it) })

/**
 * A delegate that throws
 */
fun <R : Any> ReadWriteProperty<Any?, R?>.notNull(default: () -> R): ReadWriteProperty<Any?, R> {
    return ReadWriteDelegateWrapper(this,
        reader = { it ?: default() },
        writer = { it }
    )
}


fun <M : MutableMeta<M>> M.item(default: Any? = null, key: Name? = null): MutableMetaDelegate<M> =
    MutableMetaDelegate(this, key, default?.let { MetaItem.of(it) })

fun <M : MutableMeta<M>> M.lazyItem(key: Name? = null, defaultProvider: () -> Any?): LazyMutableMetaDelegate<M> =
    LazyMutableMetaDelegate(this, key) { defaultProvider()?.let { MetaItem.of(it) } }

//Read-write delegates

/**
 * A property delegate that uses custom key
 */
fun <M : MutableMeta<M>> M.value(default: Value? = null, key: Name? = null): ReadWriteProperty<Any?, Value?> =
    item(default, key).transform { it.value }

fun <M : MutableMeta<M>> M.string(default: String? = null, key: Name? = null): ReadWriteProperty<Any?, String?> =
    item(default, key).transform { it.string }

fun <M : MutableMeta<M>> M.boolean(default: Boolean? = null, key: Name? = null): ReadWriteProperty<Any?, Boolean?> =
    item(default, key).transform { it.boolean }

fun <M : MutableMeta<M>> M.number(default: Number? = null, key: Name? = null): ReadWriteProperty<Any?, Number?> =
    item(default, key).transform { it.number }

inline fun <reified M : MutableMeta<M>> M.node(key: Name? = null) =
    item(this, key).transform { it.node as? M }

@JvmName("safeString")
fun <M : MutableMeta<M>> M.string(default: String, key: Name? = null) =
    item(default, key).transform { it.string!! }

@JvmName("safeBoolean")
fun <M : MutableMeta<M>> M.boolean(default: Boolean, key: Name? = null) =
    item(default, key).transform { it.boolean!! }

@JvmName("safeNumber")
fun <M : MutableMeta<M>> M.number(default: Number, key: Name? = null) =
    item(default, key).transform { it.number!! }

@JvmName("lazyString")
fun <M : MutableMeta<M>> M.string(key: Name? = null, default: () -> String) =
    lazyItem(key, default).transform { it.string!! }

@JvmName("safeBoolean")
fun <M : MutableMeta<M>> M.boolean(key: Name? = null, default: () -> Boolean) =
    lazyItem(key, default).transform { it.boolean!! }

@JvmName("safeNumber")
fun <M : MutableMeta<M>> M.number(key: Name? = null, default: () -> Number) =
    lazyItem(key, default).transform { it.number!! }


inline fun <M : MutableMeta<M>, reified E : Enum<E>> M.enum(default: E, key: Name? = null) =
    item(default, key).transform { it.enum<E>()!! }