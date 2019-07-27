package hep.dataforge.io

import kotlin.reflect.KClass

/**
 * A descriptor for specific type of functions
 */
interface FunctionSpec<T : Any, R : Any> {
    val inputType: KClass<T>
    val outputType: KClass<R>
}

/**
 * A server that could produce asynchronous function values
 */
interface FunctionServer {
    /**
     * Call a function with given name and descriptor
     */
    suspend fun <T : Any, R : Any, D : FunctionSpec<T, R>> call(name: String, descriptor: D, arg: T): R

    /**
     * Resolve a function descriptor for given types
     */
    fun <T : Any, R : Any> resolveType(inputType: KClass<out T>, outputType: KClass<out R>): FunctionSpec<T, R>

    /**
     * Get a generic suspended function with given name and descriptor
     */
    operator fun <T : Any, R : Any, D : FunctionSpec<T, R>> get(name: String, descriptor: D): (suspend (T) -> R) =
        { call(name, descriptor, it) }
}

suspend inline fun <reified T : Any, reified R : Any> FunctionServer.call(name: String, arg: T): R =
    call(name, resolveType(T::class, R::class), arg)

inline operator fun <reified T : Any, reified R : Any> FunctionServer.get(name: String): (suspend (T) -> R) =
    get(name, resolveType(T::class, R::class))
