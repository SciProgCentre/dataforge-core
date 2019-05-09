package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * A meta object with read-only meta base and changeable configuration on top of it
 * @param base - unchangeable base
 * @param style - the style
 */
class Styled(val base: Meta, val style: Config = Config().empty()) : MutableMeta<Styled> {
    override val items: Map<NameToken, MetaItem<Styled>>
        get() = (base.items.keys + style.items.keys).associate { key ->
            val value = base.items[key]
            val styleValue = style[key]
            val item: MetaItem<Styled> = when (value) {
                null -> when (styleValue) {
                    null -> error("Should be unreachable")
                    is MetaItem.ValueItem -> MetaItem.ValueItem(styleValue.value)
                    is MetaItem.NodeItem -> MetaItem.NodeItem(Styled(style.empty(), styleValue.node))
                }
                is MetaItem.ValueItem -> MetaItem.ValueItem(value.value)
                is MetaItem.NodeItem -> MetaItem.NodeItem(
                    Styled(value.node, styleValue?.node ?: Config.empty())
                )
            }
            key to item
        }

    override fun set(name: Name, item: MetaItem<Styled>?) {
        if (item == null) {
            style.remove(name)
        } else {
            style[name] = item
        }
    }

    override fun onChange(owner: Any?, action: (Name, before: MetaItem<*>?, after: MetaItem<*>?) -> Unit) {
        //TODO test correct behavior
        style.onChange(owner) { name, before, after -> action(name, before ?: base[name], after ?: base[name]) }
    }

    override fun removeListener(owner: Any?) {
        style.removeListener(owner)
    }
}

fun Styled.configure(meta: Meta) = apply { style.update(meta) }

fun Meta.withStyle(style: Meta = EmptyMeta) = if (this is Styled) {
    this.apply { this.configure(style) }
} else {
    Styled(this, style.toConfig())
}

class StyledNodeDelegate(val owner: Styled, val key: String?) : ReadWriteProperty<Any?, Meta> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Meta {
        return owner[key ?: property.name]?.node ?: EmptyMeta
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Meta) {
        owner.style[key ?: property.name] = value
    }

}