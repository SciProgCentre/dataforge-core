package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * Get all items matching given name. The index of the last element, if present is used as a [Regex],
 * against which indexes of elements are matched.
 */
fun Meta.getIndexed(name: Name): Map<String, MetaItem<*>> {
    val root = when (name.length) {
        0 -> error("Can't use empty name for 'getIndexed'")
        1 -> this
        else -> this[name.cutLast()].node
    }

    val (body, index) = name.last()!!
    val regex = index.toRegex()

    return root?.items
        ?.filter { it.key.body == body && (index.isEmpty() || regex.matches(it.key.index)) }
        ?.mapKeys { it.key.index }
        ?: emptyMap()
}

fun Meta.getIndexed(name: String): Map<String, MetaItem<*>> = this@getIndexed.getIndexed(name.toName())

/**
 * Get all items matching given name.
 */
@Suppress("UNCHECKED_CAST")
fun <M : MetaNode<M>> M.getIndexed(name: Name): Map<String, MetaItem<M>> =
    (this as Meta).getIndexed(name) as Map<String, MetaItem<M>>

fun <M : MetaNode<M>> M.getIndexed(name: String): Map<String, MetaItem<M>> = getIndexed(name.toName())