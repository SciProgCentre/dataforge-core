package hep.dataforge.data

import kotlin.reflect.KClass

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
actual fun DataNode<*>.checkType(type: KClass<*>) {
    //Not supported in js yet
}