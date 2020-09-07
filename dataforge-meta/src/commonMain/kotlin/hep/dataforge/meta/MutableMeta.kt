package hep.dataforge.meta

import hep.dataforge.names.*
import hep.dataforge.values.Value

public interface MutableItemProvider : ItemProvider {
    public fun setItem(name: Name, item: MetaItem<*>?)
}

public interface MutableMeta<out M : MutableMeta<M>> : MetaNode<M>, MutableItemProvider {
    override val items: Map<NameToken, MetaItem<M>>
//    fun onChange(owner: Any? = null, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit)
//    fun removeListener(owner: Any? = null)
}

/**
 * A mutable meta node with attachable change listener.
 *
 * Changes in Meta are not thread safe.
 */
public abstract class AbstractMutableMeta<M : MutableMeta<M>> : AbstractMetaNode<M>(), MutableMeta<M> {
    protected val _items: MutableMap<NameToken, MetaItem<M>> = LinkedHashMap()

    override val items: Map<NameToken, MetaItem<M>>
        get() = _items

    //protected abstract fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?)

    protected open fun replaceItem(key: NameToken, oldItem: MetaItem<M>?, newItem: MetaItem<M>?) {
        if (newItem == null) {
            _items.remove(key)
        } else {
            _items[key] = newItem
        }
        //itemChanged(key.asName(), oldItem, newItem)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun wrapItem(item: MetaItem<*>?): MetaItem<M>? = when (item) {
        null -> null
        is MetaItem.ValueItem -> item
        is MetaItem.NodeItem -> MetaItem.NodeItem(wrapNode(item.node))
    }

    /**
     * Transform given meta to node type of this meta tree
     */
    protected abstract fun wrapNode(meta: Meta): M

    /**
     * Create empty node
     */
    internal abstract fun empty(): M

    override fun setItem(name: Name, item: MetaItem<*>?) {
        when (name.length) {
            0 -> error("Can't setValue meta item for empty name")
            1 -> {
                val token = name.firstOrNull()!!
                @Suppress("UNCHECKED_CAST") val oldItem: MetaItem<M>? = get(name) as? MetaItem<M>
                replaceItem(token, oldItem, wrapItem(item))
            }
            else -> {
                val token = name.firstOrNull()!!
                //get existing or create new node. Query is ignored for new node
                if (items[token] == null) {
                    replaceItem(token, null, MetaItem.NodeItem(empty()))
                }
                items[token]?.node!!.setItem(name.cutFirst(), item)
            }
        }
    }
}


@Suppress("NOTHING_TO_INLINE")
public inline fun MutableMeta<*>.remove(name: Name): Unit = setItem(name, null)

@Suppress("NOTHING_TO_INLINE")
public inline fun MutableMeta<*>.remove(name: String): Unit = remove(name.toName())

public operator fun MutableMeta<*>.set(name: Name, item: MetaItem<*>?): Unit = setItem(name, item)

public fun MutableMeta<*>.setValue(name: Name, value: Value): Unit = setItem(name, MetaItem.ValueItem(value))

public fun MutableMeta<*>.setValue(name: String, value: Value): Unit = set(name.toName(), value)

public fun MutableMeta<*>.setItem(name: String, item: MetaItem<*>?): Unit = setItem(name.toName(), item)

public fun MutableMeta<*>.setNode(name: Name, node: Meta): Unit =
    setItem(name, MetaItem.NodeItem(node))

public fun MutableMeta<*>.setNode(name: String, node: Meta): Unit = setNode(name.toName(), node)

/**
 * Universal unsafe set method
 */
public operator fun MutableMeta<*>.set(name: Name, value: Any?) {
    when (value) {
        null -> remove(name)
        is MetaItem<*> -> setItem(name, value)
        is Meta -> setNode(name, value)
        is Configurable -> setNode(name, value.config)
        else -> setValue(name, Value.of(value))
    }
}

public operator fun MutableMeta<*>.set(name: NameToken, value: Any?): Unit =
    set(name.asName(), value)

public operator fun MutableMeta<*>.set(key: String, value: Any?): Unit =
    set(key.toName(), value)

public operator fun MutableMeta<*>.set(key: String, index: String, value: Any?): Unit =
    set(key.toName().withIndex(index), value)

/**
 * Update existing mutable node with another node. The rules are following:
 *  * value replaces anything
 *  * node updates node and replaces anything but node
 *  * node list updates node list if number of nodes in the list is the same and replaces anything otherwise
 */
public fun <M : MutableMeta<M>> M.update(meta: Meta) {
    meta.items.forEach { entry ->
        when (val value = entry.value) {
            is MetaItem.ValueItem -> setValue(entry.key.asName(), value.value)
            is MetaItem.NodeItem -> (this[entry.key.asName()] as? MetaItem.NodeItem)?.node?.update(value.node)
                ?: run { setNode(entry.key.asName(), value.node) }
        }
    }
}

/* Same name siblings generation */

public fun MutableMeta<*>.setIndexedItems(
    name: Name,
    items: Iterable<MetaItem<*>>,
    indexFactory: (MetaItem<*>, index: Int) -> String = { _, index -> index.toString() }
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    items.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, last.index + indexFactory(meta, index))
        tokens[tokens.lastIndex] = indexedToken
        setItem(Name(tokens), meta)
    }
}

public fun MutableMeta<*>.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    indexFactory: (Meta, index: Int) -> String = { _, index -> index.toString() }
) {
    setIndexedItems(name, metas.map { MetaItem.NodeItem(it) }) { item, index -> indexFactory(item.node!!, index) }
}

public operator fun MutableMeta<*>.set(name: Name, metas: Iterable<Meta>): Unit = setIndexed(name, metas)
public operator fun MutableMeta<*>.set(name: String, metas: Iterable<Meta>): Unit = setIndexed(name.toName(), metas)

/**
 * Append the node with a same-name-sibling, automatically generating numerical index
 */
public fun <M : MutableMeta<M>> M.append(name: Name, value: Any?) {
    require(!name.isEmpty()) { "Name could not be empty for append operation" }
    val newIndex = name.lastOrNull()!!.index
    if (newIndex != null) {
        set(name, value)
    } else {
        val index = (getIndexed(name).keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: -1) + 1
        set(name.withIndex(index.toString()), value)
    }
}

public fun <M : MutableMeta<M>> M.append(name: String, value: Any?): Unit = append(name.toName(), value)

/**
 * Apply existing node with given [builder] or create a new element with it.
 */
@DFExperimental
public fun <M : AbstractMutableMeta<M>> M.edit(name: Name, builder: M.() -> Unit) {
    val item = when (val existingItem = get(name)) {
        null -> empty().also { set(name, it) }
        is MetaItem.NodeItem<M> -> existingItem.node
        else -> error("Can't edit value meta item")
    }
    item.apply(builder)
}