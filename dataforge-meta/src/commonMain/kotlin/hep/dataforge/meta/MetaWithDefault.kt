package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken

/**
 * Meta object with default provider for items not present in the initial meta.
 */
public class MetaWithDefault(public val meta: Meta, public val default: ItemProvider) : MetaBase() {
    override val items: Map<NameToken, MetaItem<*>>
        get() = meta.items

    override fun getItem(name: Name): MetaItem<*>? {
        return meta[name] ?: default[name]
    }
}

public fun Meta.withDefault(default: ItemProvider): MetaWithDefault = MetaWithDefault(this, default)