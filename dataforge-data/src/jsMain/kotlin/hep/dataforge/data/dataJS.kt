package hep.dataforge.data

import kotlin.reflect.KClass

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
actual fun <T : Any> DataNode<*>.checkType(type: KClass<out T>) {
    //Not supported in js yet
}

///**
// * Performing
// */
//@Suppress("UNCHECKED_CAST")
//actual fun <T : Any, R : Any> DataNode<T>.cast(type: KClass<out R>): DataNode<R>{
//    return this as DataNode<R>
//}