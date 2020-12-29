package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.names.NameToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Block the thread and get data content
 */
public fun <T : Any> Data<T>.get(): T = runBlocking { await() }

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
internal fun <R : Any> DataNode<*>.canCast(type: KClass<out R>): Boolean =
    type.isSubclassOf(this.type)
/**
 * Check if data could be safely cast to given class
 */
internal fun <R : Any> Data<*>.canCast(type: KClass<out R>): Boolean =
    this.type.isSubclassOf(type)


public fun <R : Any, T : R> Data<T>.upcast(type: KClass<out R>): Data<R> {
    return object : Data<R> by this {
        override val type: KClass<out R> = type
    }
}

/**
 * Safe upcast a [Data] to a supertype
 */
public inline fun <reified R : Any, T : R> Data<T>.upcast(): Data<R> = upcast(R::class)

public fun <R : Any> DataItem<*>.canCast(type: KClass<out R>): Boolean = when (this) {
    is DataItem.Node -> node.canCast(type)
    is DataItem.Leaf -> data.canCast(type)
}

/**
 * Unsafe cast of data node
 */
@Suppress("UNCHECKED_CAST")
public fun <R : Any> Data<*>.cast(type: KClass<out R>): Data<R> {
    if(!canCast(type)) error("Can't cast ${this.type} to $type")
    return object : Data<R> {
        override val meta: Meta get() = this@cast.meta
        override val dependencies: Collection<Goal<*>> get() = this@cast.dependencies
        override val result: Deferred<R>? get() = this@cast.result as Deferred<R>?
        override fun startAsync(coroutineScope: CoroutineScope): Deferred<R> = this@cast.run {
            startAsync(coroutineScope) as Deferred<R>
        }

        override fun reset() = this@cast.reset()
        override val type: KClass<out R> = type
    }
}

public inline fun <reified R : Any> Data<*>.cast(): Data<R> = cast(R::class)

@Suppress("UNCHECKED_CAST")
public fun <R : Any> DataNode<*>.cast(type: KClass<out R>): DataNode<R> {
    return object : DataNode<R> {
        override val meta: Meta get() = this@cast.meta
        override val type: KClass<out R> = type
        override val items: Map<NameToken, DataItem<R>> get() = this@cast.items as Map<NameToken, DataItem<R>>
    }
}

public inline fun <reified R : Any> DataNode<*>.cast(): DataNode<R> = cast(R::class)

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
public fun <T : Any> DataNode<*>.ensureType(type: KClass<out T>) {
    if (!canCast(type)) {
        error("$type expected, but $type received")
    }
}

/**
 * Cast the node to given type if the cast is possible or return null
 */
public fun <R : Any> Data<*>.filterIsInstance(type: KClass<out R>): Data<R>? =
    if (canCast(type)) cast(type) else null

/**
 * Filter a node by data and node type. Resulting node and its subnodes is guaranteed to have border type [type],
 * but could contain empty nodes
 */
public fun <R : Any> DataNode<*>.filterIsInstance(type: KClass<out R>): DataNode<R> {
    return when {
        canCast(type) -> cast(type)
        this is TypeFilteredDataNode -> origin.filterIsInstance(type)
        else -> TypeFilteredDataNode(this, type)
    }
}

/**
 * Filter all elements of given data item that could be cast to given type. If no elements are available, return null.
 */
public fun <R : Any> DataItem<*>?.filterIsInstance(type: KClass<out R>): DataItem<R>? = when (this) {
    null -> null
    is DataItem.Node -> DataItem.Node(this.node.filterIsInstance(type))
    is DataItem.Leaf -> this.data.filterIsInstance(type)?.let { DataItem.Leaf(it) }
}

public inline fun <reified R : Any> DataItem<*>?.filterIsInstance(): DataItem<R>? = this@filterIsInstance.filterIsInstance(R::class)