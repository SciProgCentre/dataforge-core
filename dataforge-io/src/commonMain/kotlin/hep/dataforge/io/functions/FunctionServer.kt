package hep.dataforge.io.functions

import hep.dataforge.context.ContextAware
import hep.dataforge.io.IOFormat
import hep.dataforge.io.IOPlugin
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.names.asName
import hep.dataforge.names.plus


/**
 * A server that could produce asynchronous function values
 */
interface FunctionServer : ContextAware {
    /**
     * Call a function with given name and descriptor
     */
    suspend fun <T : Any, R : Any> call(meta: Meta, arg: T): R

    suspend fun <T : Any, R : Any> callMany(
        meta: Meta,
        arg: List<T>
    ): List<R> = List(arg.size) {
        call<T, R>(meta, arg[it])
    }

    /**
     * Get a generic suspended function with given name and descriptor
     */
    fun <T : Any, R : Any> function(
        meta: Meta
    ): (suspend (T) -> R) = { call(meta, it) }

    companion object {
        const val FUNCTION_NAME_KEY = "function"
        val FORMAT_KEY = "format".asName()
        val INPUT_FORMAT_KEY = FORMAT_KEY + "input"
        val OUTPUT_FORMAT_KEY = FORMAT_KEY + "output"
    }
}

fun <T : Any> IOPlugin.getInputFormat(meta: Meta): IOFormat<T> {
    return meta[FunctionServer.INPUT_FORMAT_KEY]?.let {
        resolveIOFormat(it) as IOFormat<T>
    } ?: error("Input format not resolved")
}

fun <R : Any> IOPlugin.getOutputFormat(meta: Meta): IOFormat<R> {
    return meta[FunctionServer.OUTPUT_FORMAT_KEY]?.let {
        resolveIOFormat(it) as IOFormat<R>
    } ?: error("Input format not resolved")
}


