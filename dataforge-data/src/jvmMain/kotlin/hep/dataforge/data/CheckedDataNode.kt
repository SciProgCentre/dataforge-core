package hep.dataforge.data

import hep.dataforge.names.Name
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun <T : Any, R : Any> Data<T>.safeCast(type: KClass<R>): Data<R>? {
    return if (type.isSubclassOf(type)) {
        @Suppress("UNCHECKED_CAST")
        Data.of(type, goal as Goal<R>, meta)
    } else {
        null
    }
}

/**
 * Filter a node by data and node type. Resulting node and its subnodes is guaranteed to have border type [type],
 * but could contain empty nodes
 */
fun <T : Any, R : Any> DataNode<T>.cast(type: KClass<out R>): DataNode<R> {
    return if (this is CheckedDataNode) {
        origin.cast(type)
    } else {
        CheckedDataNode(this, type)
    }
}

inline fun <T : Any, reified R : Any> DataNode<T>.cast(): DataNode<R> = cast(R::class)

class CheckedDataNode<out T : Any>(val origin: DataNode<Any>, override val type: KClass<out T>) : DataNode<T> {

    override fun get(name: Name): Data<T>? =
        origin[name]?.safeCast(type)

    override fun getNode(name: Name): DataNode<T>? {
        return origin.getNode(name)?.cast(type)
    }

    override fun data(): Sequence<Pair<Name, Data<T>>> =
        origin.data().mapNotNull { pair ->
            pair.second.safeCast(type)?.let { pair.first to it }
        }

    override fun nodes(): Sequence<Pair<Name, DataNode<T>>> =
        origin.nodes().map { it.first to it.second.cast(type) }
}