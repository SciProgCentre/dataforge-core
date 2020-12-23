package hep.dataforge.meta

import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName

public fun interface ItemProvider {
    //getItem used instead of get in order to provide extension freedom
    public fun getItem(name: Name): MetaItem<*>?

    public companion object {
        public val EMPTY: ItemProvider = ItemProvider { null }
    }
}


/* Get operations*/

/**
 * Perform recursive item search using given [name]. Each [NameToken] is treated as a name in [Meta.items] of a parent node.
 *
 * If [name] is empty return current [Meta] as a [NodeItem]
 */
public operator fun ItemProvider?.get(name: Name): MetaItem<*>? = this?.getItem(name)

/**
 * Parse [Name] from [key] using full name notation and pass it to [Meta.get]
 */
public operator fun ItemProvider?.get(key: String): MetaItem<*>? = get(key.toName())

/**
 * Create a provider that uses given provider for default values if those are not found in this provider
 */
public fun ItemProvider.withDefault(default: ItemProvider): ItemProvider = ItemProvider {
    this[it] ?: default[it]
}
