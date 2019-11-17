package hep.dataforge.data

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

/**
 * Block the thread and get data content
 */
fun <T : Any> Data<T>.get(): T = runBlocking { await() }

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
actual fun <R : Any> DataNode<*>.canCast(type: KClass<out R>): Boolean =
    type.isSuperclassOf(type)

actual fun <R : Any> Data<*>.canCast(type: KClass<out R>): Boolean =
    this.type.isSubclassOf(type)

/**
 * Cast the node to given type if the cast is possible or return null
 */
fun <R : Any> Data<*>.filterIsInstance(type: KClass<out R>): Data<R>? =
    if (canCast(type)) cast(type) else null

/**
 * Filter a node by data and node type. Resulting node and its subnodes is guaranteed to have border type [type],
 * but could contain empty nodes
 */
fun <R : Any> DataNode<*>.filterIsInstance(type: KClass<out R>): DataNode<R> {
    return when {
        canCast(type) -> cast(type)
        this is TypeFilteredDataNode -> origin.filterIsInstance(type)
        else -> TypeFilteredDataNode(this, type)
    }
}

/**
 * Filter all elements of given data item that could be cast to given type. If no elements are available, return null.
 */
fun <R : Any> DataItem<*>?.filterIsInstance(type: KClass<out R>): DataItem<R>? = when (this) {
    null -> null
    is DataItem.Node -> DataItem.Node(this.node.filterIsInstance(type))
    is DataItem.Leaf -> this.data.filterIsInstance(type)?.let { DataItem.Leaf(it) }
}

inline fun <reified R : Any> DataItem<*>?.filterIsInstance(): DataItem<R>? = this@filterIsInstance.filterIsInstance(R::class)