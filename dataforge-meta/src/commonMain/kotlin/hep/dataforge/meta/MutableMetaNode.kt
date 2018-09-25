package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName

class MetaListener(val owner: Any? = null, val action: (name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) -> Unit) {
    operator fun invoke(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) = action(name, oldItem, newItem)
}


interface MutableMeta<M : MutableMeta<M>> : Meta {
    override val items: Map<String, MetaItem<M>>
    operator fun set(name: Name, item: MetaItem<M>?)
    fun onChange(owner: Any? = null, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit)
    fun removeListener(owner: Any)
}

/**
 * A mutable meta node with attachable change listener
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

    private val _items: MutableMap<String, MetaItem<M>> = HashMap()

    override val items: Map<String, MetaItem<M>>
        get() = _items

    protected fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) {
        listeners.forEach { it(name, oldItem, newItem) }
    }

    protected open fun replaceItem(key: String, oldItem: MetaItem<M>?, newItem: MetaItem<M>?) {
        if (newItem == null) {
            _items.remove(key)
            oldItem?.nodes?.forEach {
                it.removeListener(this)
            }
        } else {
            _items[key] = newItem
            newItem.nodes.forEach {
                it.onChange(this) { name, oldItem, newItem ->
                    itemChanged(key.toName() + name, oldItem, newItem)
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
            0 -> error("Can't set meta item for empty name")
            1 -> {
                val token = name.first()!!
                if (token.hasQuery()) TODO("Queries are not supported in set operations on meta")
                replaceItem(token.body, get(name), item)
            }
            else -> {
                val token = name.first()!!
                //get existing or create new node. Query is ignored for new node
                val child = this.items[token.body]?.nodes?.get(token.query)
                        ?: empty().also { this[token.body.toName()] = MetaItem.SingleNodeItem(it) }
                child[name.cutFirst()] = item
            }
        }
    }


}

fun <M : MutableMeta<M>> M.remove(name: Name) = set(name, null)
fun <M : MutableMeta<M>> M.remove(name: String) = remove(name.toName())

operator fun <M : MutableMeta<M>> M.set(name: Name, value: Value) = set(name, MetaItem.ValueItem(value))
operator fun <M : MutableMetaNode<M>> M.set(name: Name, meta: Meta) = set(name, MetaItem.SingleNodeItem(wrap(name, meta)))
operator fun <M : MutableMetaNode<M>> M.set(name: Name, metas: List<Meta>) = set(name, MetaItem.MultiNodeItem(metas.map { wrap(name, it) }))

operator fun <M : MutableMeta<M>> M.set(name: String, item: MetaItem<M>) = set(name.toName(), item)
operator fun <M : MutableMeta<M>> M.set(name: String, value: Value) = set(name.toName(), MetaItem.ValueItem(value))
operator fun <M : MutableMetaNode<M>> M.set(name: String, meta: Meta) = set(name.toName(), meta)
operator fun <M : MutableMetaNode<M>> M.set(name: String, metas: List<Meta>) = set(name.toName(), metas)


/**
 * Universal set method
 */
operator fun <M : MutableMeta<M>> M.set(key: String, value: Any?) {
    when (value) {
        null -> remove(key)
        is Meta -> set(key, value)
        else -> set(key, Value.of(value))
    }
}

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
            is MetaItem.ValueItem -> this[entry.key] = value.value
            is MetaItem.SingleNodeItem -> (this[entry.key] as? MetaItem.SingleNodeItem)
                    ?.node?.update(value.node) ?: kotlin.run { this[entry.key] = value.node }
            is MetaItem.MultiNodeItem -> {
                val existing = this[entry.key]
                if (existing is MetaItem.MultiNodeItem && existing.nodes.size == value.nodes.size) {
                    existing.nodes.forEachIndexed { index, m ->
                        m.update(value.nodes[index])
                    }
                } else {
                    this[entry.key] = value.nodes
                }
            }
        }
    }
}