package hep.dataforge.data

import hep.dataforge.names.NameToken
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


fun <T : Any, R : Any> Data<T>.canCast(type: KClass<out R>): Boolean =
    this.type.isSubclassOf(type)

fun <T : Any, R : Any> Data<T>.safeCast(type: KClass<out R>): Data<R>? =
    if (canCast(type)) cast(type) else null


//inline fun <reified R : Any> Data<*>.safeCast(): Data<R>?  = safeCast(R::class)

class CastDataNode<out T : Any>(val origin: DataNode<Any>, override val type: KClass<out T>) : DataNode<T> {
    override val items: Map<NameToken, DataItem<T>> by lazy {
        origin.items.mapNotNull { (key, item) ->
            when (item) {
                is DataItem.Leaf -> {
                    (item.value.safeCast(type))?.let {
                        key to DataItem.Leaf(it)
                    }
                }
                is DataItem.Node -> {
                    key to DataItem.Node(item.value.safeCast(type))
                }
            }
        }.associate { it }
    }
}