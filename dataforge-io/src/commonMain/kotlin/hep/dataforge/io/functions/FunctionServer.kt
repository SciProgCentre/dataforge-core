package hep.dataforge.io

import hep.dataforge.context.ContextAware
import hep.dataforge.io.functions.FunctionSpec


/**
 * A server that could produce asynchronous function values
 */
interface FunctionServer : ContextAware {
    /**
     * Call a function with given name and descriptor
     */
    suspend fun <T : Any, R : Any> call(name: String, spec: FunctionSpec<T, R>, arg: T): R

    suspend fun <T : Any, R : Any> callMany(
        name: String,
        spec: FunctionSpec<T, R>,
        arg: List<T>
    ): List<R> = List(arg.size) {
        call(name, spec, arg[it])
    }

    /**
     * Get a generic suspended function with given name and descriptor
     */
    fun <T : Any, R : Any> get(
        name: String,
        spec: FunctionSpec<T, R>
    ): (suspend (T) -> R) =
        { call(name, spec, it) }
}


