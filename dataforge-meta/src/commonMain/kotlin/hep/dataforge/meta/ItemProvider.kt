package hep.dataforge.meta

import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.names.*

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
 * The root node of this item provider if it is present
 */
public val ItemProvider.rootNode: Meta? get() = get(Name.EMPTY).node

/**
 * Parse [Name] from [key] using full name notation and pass it to [Meta.get]
 */
public operator fun ItemProvider?.get(key: String): MetaItem<*>? = this?.get(key.toName())

/**
 * Create a provider that uses given provider for default values if those are not found in this provider
 */
public fun ItemProvider.withDefault(default: ItemProvider): ItemProvider = ItemProvider {
    this[it] ?: default[it]
}

/**
 * Get all items matching given name. The index of the last element, if present is used as a [Regex],
 * against which indexes of elements are matched.
 */
public fun ItemProvider.getIndexed(name: Name): Map<String?, MetaItem<*>> {
    val root: Meta = when (name.length) {
        0 -> error("Can't use empty name for 'getIndexed'")
        1 -> this.rootNode ?: return emptyMap()
        else -> this[name.cutLast()].node ?: return emptyMap()
    }

    val (body, index) = name.lastOrNull()!!
    return if (index == null) {
        root.items.filter { it.key.body == body }.mapKeys { it.key.index }
    } else {
        val regex = index.toRegex()
        root.items.filter { it.key.body == body && (regex.matches(it.key.index ?: "")) }
            .mapKeys { it.key.index }
    }
}

public fun ItemProvider.getIndexed(name: String): Map<String?, MetaItem<*>> = this@getIndexed.getIndexed(name.toName())

/**
 * Return a provider referencing a child node
 */
public fun ItemProvider.getChild(childName: Name): ItemProvider = get(childName).node ?: ItemProvider.EMPTY

public fun ItemProvider.getChild(childName: String): ItemProvider  = getChild(childName.toName())

/**
 * Get all items matching given name.
 */
@Suppress("UNCHECKED_CAST")
public fun <M : TypedMeta<M>> M.getIndexed(name: Name): Map<String, MetaItem<M>> =
    (this as Meta).getIndexed(name) as Map<String, MetaItem<M>>

public fun <M : TypedMeta<M>> M.getIndexed(name: String): Map<String, MetaItem<M>> =
    getIndexed(name.toName())
