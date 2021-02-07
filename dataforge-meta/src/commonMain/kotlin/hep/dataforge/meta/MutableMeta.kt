package hep.dataforge.meta

import hep.dataforge.misc.DFExperimental
import hep.dataforge.names.*

public interface MutableMeta<out M : MutableMeta<M>> : TypedMeta<M>, MutableItemProvider {
    override val items: Map<NameToken, TypedMetaItem<M>>
}

/**
 * A mutable meta node with attachable change listener.
 *
 * Changes in Meta are not thread safe.
 */
public abstract class AbstractMutableMeta<M : MutableMeta<M>> : AbstractTypedMeta<M>(), MutableMeta<M> {
    protected val children: MutableMap<NameToken, TypedMetaItem<M>> = LinkedHashMap()

    override val items: Map<NameToken, TypedMetaItem<M>>
        get() = children

    //protected abstract fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?)

    protected open fun replaceItem(key: NameToken, oldItem: TypedMetaItem<M>?, newItem: TypedMetaItem<M>?) {
        if (newItem == null) {
            children.remove(key)
        } else {
            children[key] = newItem
        }
        //itemChanged(key.asName(), oldItem, newItem)
    }

    private fun wrapItem(item: MetaItem?): TypedMetaItem<M>? = when (item) {
        null -> null
        is MetaItemValue -> item
        is MetaItemNode -> MetaItemNode(wrapNode(item.node))
    }

    /**
     * Transform given meta to node type of this meta tree
     */
    protected abstract fun wrapNode(meta: Meta): M

    /**
     * Create empty node
     */
    internal abstract fun empty(): M

    override fun setItem(name: Name, item: MetaItem?) {
        when (name.length) {
            0 -> error("Can't set a meta item for empty name")
            1 -> {
                val token = name.firstOrNull()!!
                val oldItem: TypedMetaItem<M>? = getItem(name)
                replaceItem(token, oldItem, wrapItem(item))
            }
            else -> {
                val token = name.firstOrNull()!!
                //get existing or create new node. Query is ignored for new node
                if (items[token] == null) {
                    replaceItem(token, null, MetaItemNode(empty()))
                }
                items[token]?.node!!.set(name.cutFirst(), item)
            }
        }
    }
}

/**
 * Append the node with a same-name-sibling, automatically generating numerical index
 */
public fun MutableItemProvider.append(name: Name, value: Any?) {
    require(!name.isEmpty()) { "Name could not be empty for append operation" }
    val newIndex = name.lastOrNull()!!.index
    if (newIndex != null) {
        set(name, value)
    } else {
        val index = (getIndexed(name).keys.mapNotNull { it?.toIntOrNull() }.maxOrNull() ?: -1) + 1
        set(name.withIndex(index.toString()), value)
    }
}

public fun MutableItemProvider.append(name: String, value: Any?): Unit = append(name.toName(), value)

/**
 * Apply existing node with given [builder] or create a new element with it.
 */
@DFExperimental
public fun <M : AbstractMutableMeta<M>> M.edit(name: Name, builder: M.() -> Unit) {
    val item = when (val existingItem = get(name)) {
        null -> empty().also { set(name, it) }
        is MetaItemNode<M> -> existingItem.node
        else -> error("Can't edit value meta item")
    }
    item.apply(builder)
}