package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.names.NameToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlin.reflect.KClass

fun <R : Any, T : R> Data<T>.upcast(type: KClass<out R>): Data<R> {
    return object : Data<R> by this {
        override val type: KClass<out R> = type
    }
}

/**
 * Safe upcast a [Data] to a supertype
 */
inline fun <reified R : Any, T : R> Data<T>.upcast(): Data<R> = upcast(R::class)

/**
 * Check if node could be safely cast to given class
 */
expect fun <R : Any> DataNode<*>.canCast(type: KClass<out R>): Boolean

/**
 * Check if data could be safely cast to given class
 */
expect fun <R : Any> Data<*>.canCast(type: KClass<out R>): Boolean

fun <R : Any> DataItem<*>.canCast(type: KClass<out R>): Boolean = when (this) {
    is DataItem.Node -> node.canCast(type)
    is DataItem.Leaf -> data.canCast(type)
}

/**
 * Unsafe cast of data node
 */
@Suppress("UNCHECKED_CAST")
fun <R : Any> Data<*>.cast(type: KClass<out R>): Data<R> {
    return object : Data<R> {
        override val meta: Meta get() = this@cast.meta
        override val dependencies: Collection<Goal<*>> get() = this@cast.dependencies
        override val result: Deferred<R>? get() = this@cast.result as Deferred<R>
        override fun CoroutineScope.startAsync(): Deferred<R> = this@cast.run { startAsync() as Deferred<R> }
        override fun reset() = this@cast.reset()
        override val type: KClass<out R> = type
    }
}

inline fun <reified R : Any> Data<*>.cast(): Data<R> = cast(R::class)

@Suppress("UNCHECKED_CAST")
fun <R : Any> DataNode<*>.cast(type: KClass<out R>): DataNode<R> {
    return object : DataNode<R> {
        override val meta: Meta get() = this@cast.meta
        override val type: KClass<out R> = type
        override val items: Map<NameToken, DataItem<R>> get() = this@cast.items as Map<NameToken, DataItem<R>>
    }
}

inline fun <reified R : Any> DataNode<*>.cast(): DataNode<R> = cast(R::class)

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
fun <T : Any> DataNode<*>.ensureType(type: KClass<out T>) {
    if (!canCast(type)) {
        error("$type expected, but $type received")
    }
}


//expect fun <T : Any, R : Any> DataNode<T>.cast(type: KClass<out R>): DataNode<R>