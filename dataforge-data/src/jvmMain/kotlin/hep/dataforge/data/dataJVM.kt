package hep.dataforge.data

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Block the thread and get data content
 */
fun <T : Any> Data<T>.get(): T = runBlocking { await() }

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
actual fun DataNode<*>.checkType(type: KClass<*>) {
    if (!type.isSuperclassOf(type)) {
        error("$type expected, but $type received")
    }
}