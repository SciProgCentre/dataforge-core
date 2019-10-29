package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * Get all items matching given name.
 */
@DFExperimental
fun Meta.getIndexed(name: Name): Map<String, MetaItem<*>> {
    val root = when (name.length) {
        0 -> error("Can't use empty name for that")
        1 -> this
        else -> (this[name.cutLast()] as? MetaItem.NodeItem<*>)?.node
    }

    val (body, index) = name.last()!!
    val regex = index.toRegex()

    return root?.items
        ?.filter { it.key.body == body && (index.isEmpty() || regex.matches(it.key.index)) }
        ?.mapKeys { it.key.index }
        ?: emptyMap()
}

@DFExperimental
fun Meta.getIndexed(name: String): Map<String, MetaItem<*>> = this@getIndexed.getIndexed(name.toName())


/**
 * Get all items matching given name.
 */
@DFExperimental
fun <M : MetaNode<M>> M.getIndexed(name: Name): Map<String, MetaItem<M>> {
    val root: MetaNode<M>? = when (name.length) {
        0 -> error("Can't use empty name for that")
        1 -> this
        else -> (this[name.cutLast()] as? MetaItem.NodeItem<M>)?.node
    }

    val (body, index) = name.last()!!
    val regex = index.toRegex()

    return root?.items
        ?.filter { it.key.body == body && (index.isEmpty() || regex.matches(it.key.index)) }
        ?.mapKeys { it.key.index }
        ?: emptyMap()
}

@DFExperimental
fun <M : MetaNode<M>> M.getIndexed(name: String): Map<String, MetaItem<M>> = getIndexed(name.toName())