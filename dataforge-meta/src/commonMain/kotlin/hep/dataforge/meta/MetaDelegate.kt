package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.Value
import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* Meta delegates */

open class MetaDelegate(
    open val owner: Meta,
    val key: Name? = null,
    open val default: MetaItem<*>? = null
) : ReadOnlyProperty<Any?, MetaItem<*>?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): MetaItem<*>? {
        return owner[key ?: property.name.asName()] ?: default
    }
}

class LazyMetaDelegate(
    owner: Meta,
    key: Name? = null,
    defaultProvider: () -> MetaItem<*>? = { null }
) : MetaDelegate(owner, key) {
    override val default by lazy(defaultProvider)
}

class DelegateWrapper<T, R>(
    val delegate: ReadOnlyProperty<Any?, T>,
    val reader: (T) -> R
) : ReadOnlyProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R {
        return reader(delegate.getValue(thisRef, property))
    }
}

fun <T, R> ReadOnlyProperty<Any?, T>.map(reader: (T) -> R): DelegateWrapper<T, R> =
    DelegateWrapper(this, reader)


fun Meta.item(default: Any? = null, key: Name? = null): MetaDelegate =
    MetaDelegate(this, key, default?.let { MetaItem.of(it) })

fun Meta.lazyItem(key: Name? = null, defaultProvider: () -> Any?): LazyMetaDelegate =
    LazyMetaDelegate(this, key) { defaultProvider()?.let { MetaItem.of(it) } }

//TODO add caching for sealed nodes


//Read-only delegates for Metas

/**
 * A property delegate that uses custom key
 */
fun Meta.value(default: Value? = null, key: Name? = null) =
    item(default, key).map { it.value }

fun Meta.string(default: String? = null, key: Name? = null) =
    item(default, key).map { it.string }

fun Meta.boolean(default: Boolean? = null, key: Name? = null) =
    item(default, key).map { it.boolean }

fun Meta.number(default: Number? = null, key: Name? = null) =
    item(default, key).map { it.number }

fun Meta.node(key: Name? = null) =
    item(key).map { it.node }

@JvmName("safeString")
fun Meta.string(default: String, key: Name? = null) =
    item(default, key).map { it.string!! }

@JvmName("safeBoolean")
fun Meta.boolean(default: Boolean, key: Name? = null) =
    item(default, key).map { it.boolean!! }

@JvmName("safeNumber")
fun Meta.number(default: Number, key: Name? = null) =
    item(default, key).map { it.number!! }

@JvmName("lazyString")
fun Meta.string(key: Name? = null, default: () -> String) =
    lazyItem(key, default).map { it.string!! }

@JvmName("lazyBoolean")
fun Meta.boolean(key: Name? = null, default: () -> Boolean) =
    lazyItem(key, default).map { it.boolean!! }

@JvmName("lazyNumber")
fun Meta.number(key: Name? = null, default: () -> Number) =
    lazyItem(key, default).map { it.number!! }


inline fun <reified E : Enum<E>> Meta.enum(default: E, key: Name? = null) =
    item(default, key).map { it.enum<E>()!! }