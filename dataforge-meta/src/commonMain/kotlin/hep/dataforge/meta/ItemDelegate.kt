package hep.dataforge.meta

import hep.dataforge.meta.transformations.MetaConverter
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.Value
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* Meta delegates */

open class ItemDelegate(
    open val owner: ItemProvider,
    val key: Name? = null,
    open val default: MetaItem<*>? = null
) : ReadOnlyProperty<Any?, MetaItem<*>?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): MetaItem<*>? {
        return owner.getItem(key ?: property.name.asName()) ?: default
    }
}

fun ItemProvider.item(key: Name? = null): ItemDelegate = ItemDelegate(this, key)

//TODO add caching for sealed nodes


//Read-only delegates for Metas

/**
 * A property delegate that uses custom key
 */
fun ItemProvider.value(key: Name? = null): ReadOnlyProperty<Any?, Value?> =
    item(key).convert(MetaConverter.value)

fun ItemProvider.string(key: Name? = null): ReadOnlyProperty<Any?, String?> =
    item(key).convert(MetaConverter.string)

fun ItemProvider.boolean(key: Name? = null): ReadOnlyProperty<Any?, Boolean?> =
    item(key).convert(MetaConverter.boolean)

fun ItemProvider.number(key: Name? = null): ReadOnlyProperty<Any?, Number?> =
    item(key).convert(MetaConverter.number)

fun ItemProvider.node(key: Name? = null): ReadOnlyProperty<Any?, Meta?> =
    item(key).convert(MetaConverter.meta)

fun ItemProvider.string(default: String, key: Name? = null): ReadOnlyProperty<Any?, String> =
    item(key).convert(MetaConverter.string) { default }

fun ItemProvider.boolean(default: Boolean, key: Name? = null): ReadOnlyProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean) { default }

fun ItemProvider.number(default: Number, key: Name? = null): ReadOnlyProperty<Any?, Number> =
    item(key).convert(MetaConverter.number) { default }

inline fun <reified E : Enum<E>> ItemProvider.enum(default: E, key: Name? = null): ReadOnlyProperty<Any?, E> =
    item(key).convert(MetaConverter.enum()) { default }

fun ItemProvider.string(key: Name? = null, default: () -> String): ReadOnlyProperty<Any?, String> =
    item(key).convert(MetaConverter.string, default)

fun ItemProvider.boolean(key: Name? = null, default: () -> Boolean): ReadOnlyProperty<Any?, Boolean> =
    item(key).convert(MetaConverter.boolean, default)

fun ItemProvider.number(key: Name? = null, default: () -> Number): ReadOnlyProperty<Any?, Number> =
    item(key).convert(MetaConverter.number, default)
