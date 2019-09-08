package hep.dataforge.io

import hep.dataforge.io.functions.FunctionServer
import hep.dataforge.io.functions.FunctionServer.Companion.FUNCTION_NAME_KEY
import hep.dataforge.io.functions.FunctionServer.Companion.INPUT_FORMAT_KEY
import hep.dataforge.io.functions.FunctionServer.Companion.OUTPUT_FORMAT_KEY
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.Name
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

inline fun <reified T : Any> IOPlugin.resolveIOFormat(): IOFormat<T>? {
    return ioFormats.values.find { it.type.isSuperclassOf(T::class) } as IOFormat<T>?
}

fun IOPlugin.resolveIOFormatName(type: KClass<*>): Name {
    return ioFormats.entries.find { it.value.type.isSuperclassOf(type) }?.key
        ?: error("Can't resolve IOFormat for type $type")
}

inline fun <reified T : Any, reified R : Any> IOPlugin.generateFunctionMeta(functionName: String): Meta = buildMeta {
    FUNCTION_NAME_KEY to functionName
    INPUT_FORMAT_KEY to resolveIOFormatName(T::class)
    OUTPUT_FORMAT_KEY to resolveIOFormatName(R::class)
}

inline fun <reified T : Any, reified R : Any> FunctionServer.function(
    functionName: String
): (suspend (T) -> R) {
    val plugin = context.plugins.get<IOPlugin>() ?: error("IO plugin not loaded")
    val meta = plugin.generateFunctionMeta<T, R>(functionName)
    return function(meta)
}