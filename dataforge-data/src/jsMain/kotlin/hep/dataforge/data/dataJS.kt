package hep.dataforge.data

import kotlin.reflect.KClass

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
actual fun <R : Any> DataNode<*>.canCast(type: KClass<out R>): Boolean {
    //Not supported in js yet
    return true
}

actual fun <R : Any> Data<*>.canCast(type: KClass<out R>): Boolean {
    //Not supported in js yet
    return true
}
