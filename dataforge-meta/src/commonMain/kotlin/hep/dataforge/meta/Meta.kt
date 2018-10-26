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
    data class ValueItem<M : Meta>(val value: Value) : MetaItem<M>()
    data class SingleNodeItem<M : Meta>(val node: M) : MetaItem<M>()
    data class MultiNodeItem<M : Meta>(val nodes: List<M>) : MetaItem<M>()
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Meta) return false

        return this.items == other.items
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }
}

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
class SealedMeta internal constructor(override val items: Map<String, MetaItem<SealedMeta>>) : MetaNode<SealedMeta>() {

    companion object {
        fun seal(meta: Meta): SealedMeta {
            val items = if (meta is SealedMeta) {
                meta.items
            } else {
                meta.items.mapValues { entry ->
                    val item = entry.value
                    when (item) {
                        is MetaItem.ValueItem -> MetaItem.ValueItem(item.value)
                        is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(seal(item.node))
                        is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(item.nodes.map { seal(it) })
                    }
                }
            }
            return SealedMeta(items)
        }
    }
}

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta.seal(this)

object EmptyMeta : Meta {
    override val items: Map<String, MetaItem<out Meta>> = emptyMap()
}

/**
 * Unsafe methods to access values and nodes directly from [MetaItem]
 */

val MetaItem<*>.value
    get() = (this as? MetaItem.ValueItem)?.value ?: error("Trying to interpret node meta item as value item")
val MetaItem<*>.string get() = value.string
val MetaItem<*>.boolean get() = value.boolean
val MetaItem<*>.number get() = value.number
val MetaItem<*>.double get() = number.toDouble()
val MetaItem<*>.int get() = number.toInt()
val MetaItem<*>.long get() = number.toLong()

val <M : Meta> MetaItem<M>.node: M
    get() = when (this) {
        is MetaItem.ValueItem -> error("Trying to interpret value meta item as node item")
        is MetaItem.SingleNodeItem -> node
        is MetaItem.MultiNodeItem -> nodes.first()
    }

/**
 * Utility method to access item content as list of nodes.
 * Returns empty list if it is value item.
 */
val <M : Meta> MetaItem<M>.nodes: List<M>
    get() = when (this) {
        is MetaItem.ValueItem -> emptyList()//error("Trying to interpret value meta item as node item")
        is MetaItem.SingleNodeItem -> listOf(node)
        is MetaItem.MultiNodeItem -> nodes
    }

fun <M : Meta> MetaItem<M>.indexOf(meta: M): Int {
    return when (this) {
        is MetaItem.ValueItem -> -1
        is MetaItem.SingleNodeItem -> if (node == meta) 0 else -1
        is MetaItem.MultiNodeItem -> nodes.indexOf(meta)
    }
}

/**
 * Generic meta-holder object
 */
interface Metoid {
    val meta: Meta
}