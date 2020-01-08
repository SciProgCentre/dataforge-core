package hep.dataforge.meta

import hep.dataforge.names.*
import hep.dataforge.values.Value

interface MutableMeta<out M : MutableMeta<M>> : MetaNode<M> {
    override val items: Map<NameToken, MetaItem<M>>
    operator fun set(name: Name, item: MetaItem<*>?)
//    fun onChange(owner: Any? = null, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit)
//    fun removeListener(owner: Any? = null)
}

/**
 * A mutable meta node with attachable change listener.
 *
 * Changes in Meta are not thread safe.
 */
abstract class AbstractMutableMeta<M : MutableMeta<M>> : AbstractMetaNode<M>(), MutableMeta<M> {
    protected val _items: MutableMap<NameToken, MetaItem<M>> = HashMap()

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

    override operator fun set(name: Name, item: MetaItem<*>?) {
        when (name.length) {
            0 -> error("Can't setValue meta item for empty name")
            1 -> {
                val token = name.first()!!
                @Suppress("UNCHECKED_CAST") val oldItem: MetaItem<M>? = get(name) as? MetaItem<M>
                replaceItem(token, oldItem, wrapItem(item))
            }
            else -> {
                val token = name.first()!!
                //get existing or create new node. Query is ignored for new node
                if (items[token] == null) {
                    replaceItem(token, null, MetaItem.NodeItem(empty()))
                }
                items[token]?.node!![name.cutFirst()] = item
            }
        }
    }
}


@Suppress("NOTHING_TO_INLINE")
inline fun MutableMeta<*>.remove(name: Name) = set(name, null)

@Suppress("NOTHING_TO_INLINE")
inline fun MutableMeta<*>.remove(name: String) = remove(name.toName())

fun MutableMeta<*>.setValue(name: Name, value: Value) =
    set(name, MetaItem.ValueItem(value))

fun MutableMeta<*>.setValue(name: String, value: Value) =
    set(name.toName(), MetaItem.ValueItem(value))

fun MutableMeta<*>.setItem(name: Name, item: MetaItem<*>?) {
    when (item) {
        null -> remove(name)
        is MetaItem.ValueItem -> setValue(name, item.value)
        is MetaItem.NodeItem<*> -> setNode(name, item.node)
    }
}

fun MutableMeta<*>.setItem(name: String, item: MetaItem<*>?) = setItem(name.toName(), item)

fun MutableMeta<*>.setNode(name: Name, node: Meta) =
    set(name, MetaItem.NodeItem(node))

fun MutableMeta<*>.setNode(name: String, node: Meta) = setNode(name.toName(), node)

/**
 * Universal set method
 */
operator fun MutableMeta<*>.set(name: Name, value: Any?) {
    when (value) {
        null -> remove(name)
        is MetaItem<*> -> setItem(name, value)
        is Meta -> setNode(name, value)
        is Configurable -> setNode(name, value.config)
        else -> setValue(name, Value.of(value))
    }
}

operator fun MutableMeta<*>.set(name: NameToken, value: Any?) = set(name.asName(), value)

operator fun MutableMeta<*>.set(key: String, value: Any?) = set(key.toName(), value)

/**
 * Update existing mutable node with another node. The rules are following:
 *  * value replaces anything
 *  * node updates node and replaces anything but node
 *  * node list updates node list if number of nodes in the list is the same and replaces anything otherwise
 */
fun <M : MutableMeta<M>> M.update(meta: Meta) {
    meta.items.forEach { entry ->
        when (val value = entry.value) {
            is MetaItem.ValueItem -> setValue(entry.key.asName(), value.value)
            is MetaItem.NodeItem -> (this[entry.key.asName()] as? MetaItem.NodeItem)?.node?.update(value.node)
                ?: run { setNode(entry.key.asName(), value.node) }
        }
    }
}

/* Same name siblings generation */

fun MutableMeta<*>.setIndexedItems(
    name: Name,
    items: Iterable<MetaItem<*>>,
    indexFactory: MetaItem<*>.(index: Int) -> String = { it.toString() }
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    items.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, last.index + meta.indexFactory(index))
        tokens[tokens.lastIndex] = indexedToken
        set(Name(tokens), meta)
    }
}

fun MutableMeta<*>.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    indexFactory: MetaItem<*>.(index: Int) -> String = { it.toString() }
) {
    setIndexedItems(name, metas.map { MetaItem.NodeItem(it) }, indexFactory)
}

operator fun MutableMeta<*>.set(name: Name, metas: Iterable<Meta>): Unit = setIndexed(name, metas)
operator fun MutableMeta<*>.set(name: String, metas: Iterable<Meta>): Unit = setIndexed(name.toName(), metas)

/**
 * Append the node with a same-name-sibling, automatically generating numerical index
 */
fun MutableMeta<*>.append(name: Name, value: Any?) {
    require(!name.isEmpty()) { "Name could not be empty for append operation" }
    val newIndex = name.last()!!.index
    if (newIndex.isNotEmpty()) {
        set(name, value)
    } else {
        val index = (getIndexed(name).keys.mapNotNull { it.toIntOrNull() }.max() ?: -1) + 1
        set(name.withIndex(index.toString()), value)
    }
}

fun MutableMeta<*>.append(name: String, value: Any?) = append(name.toName(), value)