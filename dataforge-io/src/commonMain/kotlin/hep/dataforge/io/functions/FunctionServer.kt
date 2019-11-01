package hep.dataforge.io.functions

import hep.dataforge.context.ContextAware
import hep.dataforge.io.IOFormat
import hep.dataforge.io.IOPlugin
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.names.asName
import hep.dataforge.names.plus
import kotlin.reflect.KClass


/**
 * A server that could produce asynchronous function values
 */
interface FunctionServer : ContextAware {
    /**
     * Call a function with given name and descriptor
     */
    suspend fun <T : Any, R : Any> call(meta: Meta, arg: T, inputType: KClass<out T>, outputType: KClass<out R>): R

    suspend fun <T : Any, R : Any> callMany(
        meta: Meta,
        arg: List<T>,
        inputType: KClass<out T>,
        outputType: KClass<out R>
    ): List<R> = List(arg.size) {
        call<T, R>(meta, arg[it], inputType, outputType)
    }

    /**
     * Get a generic suspended function with given name and descriptor
     */
    fun <T : Any, R : Any> function(
        meta: Meta,
        inputType: KClass<out T>,
        outputType: KClass<out R>
    ): (suspend (T) -> R) = { call(meta, it, inputType, outputType) }

    companion object {
        const val FUNCTION_NAME_KEY = "function"
        val FORMAT_KEY = "format".asName()
        val INPUT_FORMAT_KEY = FORMAT_KEY + "input"
        val OUTPUT_FORMAT_KEY = FORMAT_KEY + "output"
    }
}

suspend inline fun <reified T : Any, reified R : Any> FunctionServer.call(meta: Meta, arg: T) =
    call(meta, arg, T::class, R::class)

suspend inline fun <reified T : Any, reified R : Any> FunctionServer.callMany(meta: Meta, arg: List<T>) =
    callMany(meta, arg, T::class, R::class)

inline fun <reified T : Any, reified R : Any> FunctionServer.function(meta: Meta) =
    function(meta, T::class, R::class)

fun <T : Any> IOPlugin.getInputFormat(meta: Meta, type: KClass<out T>): IOFormat<T> {
    return meta[FunctionServer.INPUT_FORMAT_KEY]?.let {
        resolveIOFormat<T>(it, type)
    } ?: error("Input format not resolved")
}

fun <R : Any> IOPlugin.getOutputFormat(meta: Meta, type: KClass<out R>): IOFormat<R> {
    return meta[FunctionServer.OUTPUT_FORMAT_KEY]?.let {
        resolveIOFormat<R>(it, type)
    } ?: error("Input format not resolved")
}

inline fun <reified T : Any> IOPlugin.getInputFormat(meta: Meta): IOFormat<T> =
    getInputFormat(meta, T::class)

inline fun <reified R : Any> IOPlugin.getOutputFormat(meta: Meta): IOFormat<R> =
    getOutputFormat(meta, R::class)


