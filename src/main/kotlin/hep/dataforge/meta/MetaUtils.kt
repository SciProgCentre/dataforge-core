package hep.dataforge.meta

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