package hep.dataforge.meta

import hep.dataforge.names.*
import hep.dataforge.values.Value

internal data class MetaListener(
    val owner: Any? = null,
    val action: (name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) -> Unit
)


interface MutableMeta<M : MutableMeta<M>> : MetaNode<M> {
    override val items: Map<NameToken, MetaItem<M>>
    operator fun set(name: Name, item: MetaItem<M>?)
    fun onChange(owner: Any? = null, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit)
    fun removeListener(owner: Any? = null)
}

/**
 * A mutable meta node with attachable change listener.
 *
 * Changes in Meta are not thread safe.
 */
abstract class MutableMetaNode<M : MutableMetaNode<M>> : AbstractMetaNode<M>(), MutableMeta<M> {
    private val listeners = HashSet<MetaListener>()

    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    override fun onChange(owner: Any?, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit) {
        listeners.add(MetaListener(owner, action))
    }

    /**
     * Remove all listeners belonging to given owner
     */
    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }

    private val _items: MutableMap<NameToken, MetaItem<M>> = HashMap()

    override val items: Map<NameToken, MetaItem<M>>
        get() = _items

    protected fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) {
        listeners.forEach { it.action(name, oldItem, newItem) }
    }

    protected open fun replaceItem(key: NameToken, oldItem: MetaItem<M>?, newItem: MetaItem<M>?) {
        if (newItem == null) {
            _items.remove(key)
            oldItem?.node?.removeListener(this)
        } else {
            _items[key] = newItem
            if (newItem is MetaItem.NodeItem) {
                newItem.node.onChange(this) { name, oldChild, newChild ->
                    itemChanged(key + name, oldChild, newChild)
                }
            }
        }
        itemChanged(key.asName(), oldItem, newItem)
    }

    /**
     * Transform given meta to node type of this meta tree
     * @param name the name of the node where meta should be attached. Needed for correct assignment validators and styles
     * @param meta the node itself
     */
    internal abstract fun wrap(name: Name, meta: Meta): M

    /**
     * Create empty node
     */
    internal abstract fun empty(): M

    override operator fun set(name: Name, item: MetaItem<M>?) {
        when (name.length) {
            0 -> error("Can't setValue meta item for empty name")
            1 -> {
                val token = name.first()!!
                replaceItem(token, get(name), item)
            }
            else -> {
                val token = name.first()!!
                //get existing or create new node. Query is ignored for new node
                val child = this.items[token]?.node
                    ?: empty().also { this[token.body.toName()] = MetaItem.NodeItem(it) }
                child[name.cutFirst()] = item
            }
        }
    }
}

fun <M : MutableMeta<M>> MutableMeta<M>.remove(name: Name) = set(name, null)
fun <M : MutableMeta<M>> MutableMeta<M>.remove(name: String) = remove(name.toName())

fun <M : MutableMeta<M>> MutableMeta<M>.setValue(name: Name, value: Value) = set(name, MetaItem.ValueItem(value))
fun <M : MutableMeta<M>> MutableMeta<M>.setItem(name: String, item: MetaItem<M>) = set(name.toName(), item)
fun <M : MutableMeta<M>> MutableMeta<M>.setValue(name: String, value: Value) =
    set(name.toName(), MetaItem.ValueItem(value))

fun <M : MutableMeta<M>> MutableMeta<M>.setItem(token: NameToken, item: MetaItem<M>?) = set(token.asName(), item)

fun <M : MutableMetaNode<M>> MutableMetaNode<M>.setNode(name: Name, node: Meta) =
    set(name, MetaItem.NodeItem(wrap(name, node)))

fun <M : MutableMetaNode<M>> MutableMetaNode<M>.setNode(name: String, node: Meta) = setNode(name.toName(), node)

/**
 * Universal set method
 */
operator fun <M : MutableMetaNode<M>> M.set(name: Name, value: Any?) {
    when (value) {
        null -> remove(name)
        is MetaItem<*> -> when (value) {
            is MetaItem.ValueItem<*> -> setValue(name, value.value)
            is MetaItem.NodeItem<*> -> setNode(name, value.node)
        }
        is Meta -> setNode(name, value)
        is Specific -> setNode(name, value.config)
        else -> setValue(name, Value.of(value))
    }
}

operator fun <M : MutableMetaNode<M>> M.set(name: NameToken, value: Any?) = set(name.asName(), value)

operator fun <M : MutableMetaNode<M>> M.set(key: String, value: Any?) = set(key.toName(), value)

/**
 * Update existing mutable node with another node. The rules are following:
 *  * value replaces anything
 *  * node updates node and replaces anything but node
 *  * node list updates node list if number of nodes in the list is the same and replaces anything otherwise
 */
fun <M : MutableMetaNode<M>> M.update(meta: Meta) {
    meta.items.forEach { entry ->
        val value = entry.value
        when (value) {
            is MetaItem.ValueItem -> setValue(entry.key.asName(), value.value)
            is MetaItem.NodeItem -> (this[entry.key.asName()] as? MetaItem.NodeItem)?.node?.update(value.node)
                ?: run { setNode(entry.key.asName(), value.node) }
        }
    }
}

/* Same name siblings generation */

fun <M : MutableMeta<M>> M.setIndexed(
    name: Name,
    items: Iterable<MetaItem<M>>,
    indexFactory: MetaItem<M>.(index: Int) -> String = { it.toString() }
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    items.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, last.index + meta.indexFactory(index))
        tokens[tokens.lastIndex] = indexedToken
        set(Name(tokens), meta)
    }
}

fun <M : MutableMetaNode<M>> M.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    indexFactory: MetaItem<M>.(index: Int) -> String = { it.toString() }
) {
    setIndexed(name, metas.map { MetaItem.NodeItem(wrap(name, it)) }, indexFactory)
}

operator fun <M : MutableMetaNode<M>> M.set(name: Name, metas: Iterable<Meta>) = setIndexed(name, metas)
operator fun <M : MutableMetaNode<M>> M.set(name: String, metas: Iterable<Meta>) = setIndexed(name.toName(), metas)

/**
 * Append the node with a same-name-sibling, automatically generating numerical index
 */
fun <M : MutableMetaNode<M>> M.append(name: Name, value: Any?) {
    require(!name.isEmpty()) { "Name could not be empty for append operation" }
    val newIndex = name.last()!!.index
    if (newIndex.isNotEmpty()) {
        set(name, value)
    } else {
        val index = (getAll(name).keys.mapNotNull { it.toIntOrNull() }.max() ?: -1) + 1
        set(name.withIndex(index.toString()), value)
    }
}

fun <M : MutableMetaNode<M>> M.append(name: String, value: Any?) = append(name.toName(), value)