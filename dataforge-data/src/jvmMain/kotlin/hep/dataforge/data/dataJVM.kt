package hep.dataforge.data

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Block the thread and get data content
 */
fun <T : Any> Data<T>.get(): T = runBlocking { await() }

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
actual fun <T: Any> DataNode<*>.checkType(type: KClass<out T>) {
    if (!type.isSuperclassOf(type)) {
        error("$type expected, but $type received")
    }
}

/**
 * Filter a node by data and node type. Resulting node and its subnodes is guaranteed to have border type [type],
 * but could contain empty nodes
 */
fun <T : Any, R : Any> DataNode<T>.cast(type: KClass<out R>): DataNode<R> {
    return if (this is CastDataNode) {
        origin.cast(type)
    } else {
        CastDataNode(this, type)
    }
}

inline fun <T : Any, reified R : Any> DataNode<T>.cast(): DataNode<R> = cast(R::class)