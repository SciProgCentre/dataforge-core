package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import hep.dataforge.values.Value

class MetaListener(
    val owner: Any? = null,
    val action: (name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) -> Unit
) {
    operator fun invoke(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) = action(name, oldItem, newItem)
}


interface MutableMeta<M : MutableMeta<M>> : Meta {
    override val items: Map<NameToken, MetaItem<M>>
    operator fun set(name: Name, item: MetaItem<M>?)
    fun onChange(owner: Any? = null, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit)
    fun removeListener(owner: Any)
}

/**
 * A mutable meta node with attachable change listener.
 *
 * Changes in Meta are not thread safe.
 */
abstract class MutableMetaNode<M : MutableMetaNode<M>> : MetaNode<M>(), MutableMeta<M> {
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
    override fun removeListener(owner: Any) {
        listeners.removeAll { it.owner === owner }
    }

    private val _items: MutableMap<NameToken, MetaItem<M>> = HashMap()

    override val items: Map<NameToken, MetaItem<M>>
        get() = _items

    protected fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) {
        listeners.forEach { it(name, oldItem, newItem) }
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
        itemChanged(key.toName(), oldItem, newItem)
    }

    /**
     * Transform given meta to node type of this meta tree
     * @param name the name of the node where meta should be attached. Needed for correct assignment validators and styles
     * @param meta the node itself
     */
    abstract fun wrap(name: Name, meta: Meta): M

    /**
     * Create empty node
     */
    abstract fun empty(): M

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

fun <M : MutableMeta<M>> M.remove(name: Name) = set(name, null)
fun <M : MutableMeta<M>> M.remove(name: String) = remove(name.toName())

fun <M : MutableMeta<M>> M.setValue(name: Name, value: Value) = set(name, MetaItem.ValueItem(value))
fun <M : MutableMeta<M>> M.setItem(name: String, item: MetaItem<M>) = set(name.toName(), item)
fun <M : MutableMeta<M>> M.setValue(name: String, value: Value) = set(name.toName(), MetaItem.ValueItem(value))
fun <M : MutableMeta<M>> M.setItem(token: NameToken, item: MetaItem<M>?) = set(token.toName(), item)

fun <M : MutableMetaNode<M>> M.setNode(name: Name, node: Meta) = set(name, MetaItem.NodeItem(wrap(name, node)))
fun <M : MutableMetaNode<M>> M.setNode(name: String, node: Meta) = setNode(name.toName(), node)

/**
 * Universal set method
 */
operator fun <M : MutableMetaNode<M>> M.set(name: Name, value: Any?) {
    when (value) {
        null -> remove(name)
        is Meta -> setNode(name, value)
        else -> setValue(name, Value.of(value))
    }
}

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
            is MetaItem.ValueItem -> setValue(entry.key.toName(), value.value)
            is MetaItem.NodeItem -> (this[entry.key.toName()] as? MetaItem.NodeItem)?.node?.update(value.node)
                ?: run { setNode(entry.key.toName(), value.node) }
        }
    }
}

/* Same name siblings generation */

fun <M : MutableMeta<M>> M.setIndexed(
    name: Name,
    items: Iterable<MetaItem<M>>,
    queryFactory: (Int) -> String = { it.toString() }
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    items.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, last.query + queryFactory(index))
        tokens[tokens.lastIndex] = indexedToken
        set(Name(tokens), meta)
    }
}

fun <M : MutableMetaNode<M>> M.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    queryFactory: (Int) -> String = { it.toString() }
) {
    setIndexed(name, metas.map { MetaItem.NodeItem(wrap(name, it)) }, queryFactory)
}

operator fun <M : MutableMetaNode<M>> M.set(name: Name, metas: Iterable<Meta>) = setIndexed(name, metas)
operator fun <M : MutableMetaNode<M>> M.set(name: String, metas: Iterable<Meta>) = setIndexed(name.toName(), metas)
