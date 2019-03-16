package hep.dataforge.provider

import hep.dataforge.context.Context
import hep.dataforge.context.members
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation


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

inline fun <reified T : Any> Provider.provideAllByType(): Sequence<T> {
    val target = Types[T::class]
    return provideAll(target).filterIsInstance<T>()
}

/**
 * A sequences of all objects provided by plugins with given target and type
 */
inline fun <reified T : Any> Context.members(): Sequence<T> = members<T>(Types[T::class])

