package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.reflect.KClass

/**
 * A data element characterized by its meta
 */
interface Data<out T : Any> : MetaRepr {
    /**
     * Type marker for the data. The type is known before the calculation takes place so it could be checked.
     */
    val type: KClass<out T>
    /**
     * Meta for the data
     */
    val meta: Meta

    /**
     * Lazy data value
     */
    val task: Deferred<T>

    override fun toMeta(): Meta = meta

    companion object {
        const val TYPE = "data"

        fun <T : Any> of(type: KClass<out T>, goal: Deferred<T>, meta: Meta): Data<T> = DataImpl(type, goal, meta)

        inline fun <reified T : Any> of(goal: Deferred<T>, meta: Meta): Data<T> = of(T::class, goal, meta)

        fun <T : Any> of(name: String, type: KClass<out T>, goal: Deferred<T>, meta: Meta): Data<T> =
            NamedData(name, of(type, goal, meta))

        inline fun <reified T : Any> of(name: String, goal: Deferred<T>, meta: Meta): Data<T> =
            of(name, T::class, goal, meta)

        fun <T : Any> static(value: T, meta: Meta): Data<T> =
            DataImpl(value::class, CompletableDeferred(value), meta)
    }
}

/**
 * Upcast a [Data] to a supertype
 */
inline fun <reified R : Any, reified T : R> Data<T>.cast(): Data<R> {
    return Data.of(R::class, task, meta)
}

fun <R : Any, T : R> Data<T>.cast(type: KClass<R>): Data<R> {
    return Data.of(type, task, meta)
}

suspend fun <T : Any> Data<T>.await(): T = task.await()

/**
 * Generic Data implementation
 */
private class DataImpl<out T : Any>(
    override val type: KClass<out T>,
    override val task: Deferred<T>,
    override val meta: Meta
) : Data<T>

class NamedData<out T : Any>(val name: String, data: Data<T>) : Data<T> by data

