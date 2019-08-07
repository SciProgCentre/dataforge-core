package hep.dataforge.data

import hep.dataforge.names.NameToken
import kotlinx.coroutines.Deferred
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun <T : Any, R : Any> Data<T>.safeCast(type: KClass<R>): Data<R>? {
    return if (type.isSubclassOf(type)) {
        @Suppress("UNCHECKED_CAST")
        Data.of(type, task as Deferred<R>, meta)
    } else {
        null
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

class CastDataNode<out T : Any>(val origin: DataNode<Any>, override val type: KClass<out T>) : DataNode<T> {
    override val items: Map<NameToken, DataItem<T>>  by lazy {
        origin.items.mapNotNull { (key, item) ->
            when (item) {
                is DataItem.Leaf -> {
                    (item.value.safeCast(type))?.let {
                        key to DataItem.Leaf(it)
                    }
                }
                is DataItem.Node -> {
                    key to DataItem.Node(item.value.cast(type))
                }
            }
        }.associate { it }
    }
}