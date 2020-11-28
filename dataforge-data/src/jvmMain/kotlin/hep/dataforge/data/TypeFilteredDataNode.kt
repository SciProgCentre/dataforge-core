package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.names.NameToken
import kotlin.reflect.KClass


/**
 * A zero-copy data node wrapper that returns only children with appropriate type.
 */
public class TypeFilteredDataNode<out T : Any>(public val origin: DataNode<*>, override val type: KClass<out T>) : DataNode<T> {
    override val meta: Meta get() = origin.meta
    override val items: Map<NameToken, DataItem<T>> by lazy {
        origin.items.mapNotNull { (key, item) ->
            when (item) {
                is DataItem.Leaf -> {
                    (item.data.filterIsInstance(type))?.let {
                        key to DataItem.Leaf(it)
                    }
                }
                is DataItem.Node -> {
                    key to DataItem.Node(item.node.filterIsInstance(type))
                }
            }
        }.associate { it }
    }
}