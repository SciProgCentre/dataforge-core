package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * A member of the meta tree. Could be represented as one of following:
 * * a value
 * * a single node
 * * a list of nodes
 */
sealed class MetaItem<M : Meta> {
    class ValueItem<M : Meta>(val value: Value) : MetaItem<M>()
    class SingleNodeItem<M : Meta>(val node: M) : MetaItem<M>()
    class MultiNodeItem<M : Meta>(val nodes: List<M>) : MetaItem<M>()
}

operator fun <M : Meta> List<M>.get(query: String): M? {
    return if (query.isEmpty()) {
        first()
    } else {
        //TODO add custom key queries
        get(query.toInt())
    }
}

/**
 * Generic meta tree representation. Elements are [MetaItem] objects that could be represented by three different entities:
 *  * [MetaItem.ValueItem] (leaf)
 *  * [MetaItem.SingleNodeItem] single node
 *  * [MetaItem.MultiNodeItem] multi-value node
 */
interface Meta {
    val items: Map<String, MetaItem<out Meta>>
}

operator fun Meta.get(name: Name): MetaItem<out Meta>? {
    return when (name.length) {
        0 -> error("Can't resolve element from empty name")
        1 -> items[name.first()!!.body]
        else -> name.first()!!.let { token -> items[token.body]?.nodes?.get(token.query) }?.get(name.cutFirst())
    }
}

//TODO create Java helper for meta operations
operator fun Meta.get(key: String): MetaItem<out Meta>? = get(key.toName())

/**
 * A meta node that ensures that all of its descendants has at least the same type
 */
abstract class MetaNode<M : MetaNode<M>> : Meta {
    abstract override val items: Map<String, MetaItem<M>>

    operator fun get(name: Name): MetaItem<M>? {
        return when (name.length) {
            0 -> error("Can't resolve element from empty name")
            1 -> items[name.first()!!.body]
            else -> name.first()!!.let { token -> items[token.body]?.nodes?.get(token.query) }?.get(name.cutFirst())
        }
    }

    operator fun get(key: String): MetaItem<M>? = get(key.toName())
}

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
class SealedMeta(meta: Meta) : MetaNode<SealedMeta>() {
    override val items: Map<String, MetaItem<SealedMeta>> = if (meta is SealedMeta) {
        meta.items
    } else {
        meta.items.mapValues { entry ->
            val item = entry.value
            when (item) {
                is MetaItem.ValueItem -> MetaItem.ValueItem(item.value)
                is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(SealedMeta(item.node))
                is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(item.nodes.map { SealedMeta(it) })
            }
        }
    }
}

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(this)

object EmptyMeta : Meta {
    override val items: Map<String, MetaItem<out Meta>> = emptyMap()
}

/**
 * Generic meta-holder object
 */
interface Metoid {
    val meta: Meta
}