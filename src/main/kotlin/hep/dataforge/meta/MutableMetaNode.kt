package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName

class MetaListener(val owner: Any? = null, val action: (name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) -> Unit) {
    operator fun invoke(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) = action(name, oldItem, newItem)
}


interface MutableMeta<M : MutableMeta<M>> : Meta {
    operator fun set(name: Name, item: MetaItem<M>?)
}

/**
 * A mutable meta node with attachable change listener
 */
abstract class MutableMetaNode<M : MutableMetaNode<M>> : MetaNode<M>(), MutableMeta<M> {
    private val listeners = HashSet<MetaListener>()

    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    fun onChange(owner: Any? = null, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit) {
        listeners.add(MetaListener(owner, action))
    }

    /**
     * Remove all listeners belonging to given owner
     */
    fun removeListener(owner: Any) {
        listeners.removeAll { it.owner === owner }
    }

    private val _items: MutableMap<String, MetaItem<M>> = HashMap()

    override val items: Map<String, MetaItem<M>>
        get() = _items

    private fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) {
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
     */
    protected abstract fun wrap(meta: Meta): M

    /**
     * Create empty node
     */
    protected abstract fun empty(): M

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

    fun remove(name: String) = set(name.toName(), null)

    operator fun set(name: Name, value: Value) = set(name, MetaItem.ValueItem(value))
    operator fun set(name: Name, meta: Meta) = set(name, MetaItem.SingleNodeItem(wrap(meta)))
    operator fun set(name: Name, metas: List<Meta>) = set(name, MetaItem.MultiNodeItem(metas.map { wrap(it) }))

    operator fun set(name: String, item: MetaItem<M>) = set(name.toName(), item)
    operator fun set(name: String, value: Value) = set(name.toName(), MetaItem.ValueItem(value))
    operator fun set(name: String, meta: Meta) = set(name.toName(), MetaItem.SingleNodeItem(wrap(meta)))
    operator fun set(name: String, metas: List<Meta>) = set(name.toName(), MetaItem.MultiNodeItem(metas.map { wrap(it) }))
}