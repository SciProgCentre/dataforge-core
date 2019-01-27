package hep.dataforge.provider

import kotlin.reflect.KClass

/**
 * A text label for internal DataForge type classification. Alternative for mime container type.
 *
 * The DataForge type notation presumes that type `A.B.C` is the subtype of `A.B`
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class Type(val id: String)

/**
 * Utils to get type of classes and objects
 */
object Types {
    operator fun get(cl: KClass<*>): String {
        return cl.annotations.filterIsInstance<Type>().firstOrNull()?.id ?: cl.simpleName ?: ""
    }

    operator fun get(obj: Any): String {
        return get(obj::class)
    }
}

