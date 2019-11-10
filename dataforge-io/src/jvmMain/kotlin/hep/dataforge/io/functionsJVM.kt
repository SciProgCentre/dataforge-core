package hep.dataforge.io

import hep.dataforge.io.functions.FunctionServer
import hep.dataforge.io.functions.function
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.Name
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


fun IOPlugin.resolveIOFormatName(type: KClass<*>): Name {
    return ioFormats.entries.find { it.value.type.isSuperclassOf(type) }?.key
        ?: error("Can't resolve IOFormat for type $type")
}

inline fun <reified T : Any, reified R : Any> IOPlugin.generateFunctionMeta(functionName: String): Meta = buildMeta {
    FunctionServer.FUNCTION_NAME_KEY put functionName
    FunctionServer.INPUT_FORMAT_KEY put resolveIOFormatName(T::class).toString()
    FunctionServer.OUTPUT_FORMAT_KEY put resolveIOFormatName(R::class).toString()
}

inline fun <reified T : Any, reified R : Any> FunctionServer.function(
    functionName: String
): (suspend (T) -> R) {
    val plugin = context.plugins.get<IOPlugin>() ?: error("IO plugin not loaded")
    val meta = plugin.generateFunctionMeta<T, R>(functionName)
    return function(meta)
}