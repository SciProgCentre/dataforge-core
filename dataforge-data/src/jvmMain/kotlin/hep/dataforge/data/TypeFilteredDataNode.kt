package hep.dataforge.data

import hep.dataforge.names.NameToken
import kotlin.reflect.KClass


/**
 * A zero-copy data node wrapper that returns only children with appropriate type.
 */
class TypeFilteredDataNode<out T : Any>(val origin: DataNode<*>, override val type: KClass<out T>) : DataNode<T> {
    override val items: Map<NameToken, DataItem<T>> by lazy {
        origin.items.mapNotNull { (key, item) ->
            when (item) {
                is DataItem.Leaf -> {
                    (item.value.withType(type))?.let {
                        key to DataItem.Leaf(it)
                    }
                }
                is DataItem.Node -> {
                    key to DataItem.Node(item.value.withType(type))
                }
            }
        }.associate { it }
    }
}