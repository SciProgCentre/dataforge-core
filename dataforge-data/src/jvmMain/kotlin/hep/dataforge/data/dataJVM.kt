package hep.dataforge.data

import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Block the thread and get data content
 */
public fun <T : Any> Data<T>.value(): T = runBlocking { await() }

/**
 * Check if data could be safely cast to given class
 */
internal fun <R : Any> Data<*>.canCast(type: KClass<out R>): Boolean =
    this.type.isSubclassOf(type)


//public fun <R : Any, T : R> Data<T>.upcast(type: KClass<out R>): Data<R> {
//    return object : Data<R> by this {
//        override val type: KClass<out R> = type
//    }
//}
//
///**
// * Safe upcast a [Data] to a supertype
// */
//public inline fun <reified R : Any, T : R> Data<T>.upcast(): Data<R> = upcast(R::class)

/**
 * Cast the node to given type if the cast is possible or return null
 */
@Suppress("UNCHECKED_CAST")
public fun <R : Any> Data<*>.castOrNull(type: KClass<out R>): Data<R>? =
    if (!canCast(type)) null else object : Data<R> by (this as Data<R>) {
        override val type: KClass<out R> = type
    }

/**
 * Unsafe cast of data node
 */
public fun <R : Any> Data<*>.cast(type: KClass<out R>): Data<R> =
    castOrNull(type) ?: error("Can't cast ${this.type} to $type")

public inline fun <reified R : Any> Data<*>.cast(): Data<R> = cast(R::class)

@Suppress("UNCHECKED_CAST")
public fun <R : Any> DataSet<*>.castOrNull(type: KClass<out R>): DataSet<R>? =
    if (!canCast(type)) null else object : DataSet<R> by (this as DataSet<R>) {
        override val dataType: KClass<out R> = type
    }


public fun <R : Any> DataSet<*>.cast(type: KClass<out R>): DataSet<R> =
    castOrNull(type) ?: error("Can't cast ${this.dataType} to $type")

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
internal fun <R : Any> DataSet<*>.canCast(type: KClass<out R>): Boolean =
    type.isSubclassOf(this.dataType)


public operator fun <T : Any> DataTree<T>.get(name: Name): DataTreeItem<T>? = runBlocking {
    getItem(name)
}

public operator fun <T : Any> DataTree<T>.get(name: String): DataTreeItem<T>? = get(name.toName())