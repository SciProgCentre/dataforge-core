package hep.dataforge.provider

import hep.dataforge.context.Context
import hep.dataforge.context.content
import hep.dataforge.names.Name
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 *
 */
object Types {
    operator fun get(cl: KClass<*>): String {
        return cl.findAnnotation<Type>()?.id ?: cl.simpleName ?: ""
    }

    operator fun get(obj: Any): String {
        return get(obj::class)
    }
}

/**
 * Provide an object with given name inferring target from its type using [Type] annotation
 */
inline fun <reified T : Any> Provider.provideByType(name: String): T? {
    val target = Types[T::class]
    return provide(target, name)
}

inline fun <reified T : Any> Provider.provideByType(name: Name): T? {
    val target = Types[T::class]
    return provide(target, name)
}

inline fun <reified T : Any> Provider.top(): Map<Name, T> {
    val target = Types[T::class]
    return listNames(target).associate { name ->
        name to (provideByType<T>(name) ?: error("The element $name is declared but not provided"))
    }
}

/**
 * A sequences of all objects provided by plugins with given target and type
 */
inline fun <reified T : Any> Context.content(): Map<Name, T> = content<T>(Types[T::class])

