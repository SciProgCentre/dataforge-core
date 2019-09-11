package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.names.NameToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
fun <T : Any, R : Any> Data<T>.safeCast(type: KClass<out R>): Data<R>? {
    return if (this.type.isSubclassOf(type)) {
        return object : Data<R> {
            override val meta: Meta get() = this@safeCast.meta
            override val dependencies: Collection<Goal<*>> get() = this@safeCast.dependencies
            override val result: Deferred<R>? get() = this@safeCast.result as Deferred<R>
            override fun startAsync(scope: CoroutineScope): Deferred<R>  = this@safeCast.startAsync(scope)  as Deferred<R>
            override fun reset() = this@safeCast.reset()
            override val type: KClass<out R> = type
        }
    } else {
        null
    }
}

inline fun <reified R : Any> Data<*>.safeCast(): Data<R>?  = safeCast(R::class)

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