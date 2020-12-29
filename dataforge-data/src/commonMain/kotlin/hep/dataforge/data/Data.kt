package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.isEmpty
import hep.dataforge.type.Type
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * A data element characterized by its meta
 */
@Type(Data.TYPE)
public interface Data<out T : Any> : Goal<T>, MetaRepr {
    /**
     * Type marker for the data. The type is known before the calculation takes place so it could be checked.
     */
    public val type: KClass<out T>

    /**
     * Meta for the data
     */
    public val meta: Meta

    override fun toMeta(): Meta = Meta {
        "type" put (type.simpleName ?: "undefined")
        if (!meta.isEmpty()) {
            "meta" put meta
        }
    }

    public companion object {
        public const val TYPE: String = "data"

        public operator fun <T : Any> invoke(
            type: KClass<out T>,
            meta: Meta = Meta.EMPTY,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            block: suspend CoroutineScope.() -> T,
        ): Data<T> = ComputationData(type, meta, context, dependencies, block)

        public inline operator fun <reified T : Any> invoke(
            meta: Meta = Meta.EMPTY,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            noinline block: suspend CoroutineScope.() -> T,
        ): Data<T> = invoke(T::class, meta, context, dependencies, block)

        public operator fun <T : Any> invoke(
            name: String,
            type: KClass<out T>,
            meta: Meta = Meta.EMPTY,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            block: suspend CoroutineScope.() -> T,
        ): Data<T> = NamedData(name, invoke(type, meta, context, dependencies, block))

        public inline operator fun <reified T : Any> invoke(
            name: String,
            meta: Meta = Meta.EMPTY,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            noinline block: suspend CoroutineScope.() -> T,
        ): Data<T> =
            invoke(name, T::class, meta, context, dependencies, block)

        public fun <T : Any> static(value: T, meta: Meta = Meta.EMPTY): Data<T> =
            StaticData(value, meta)
    }
}


public class ComputationData<T : Any>(
    override val type: KClass<out T>,
    override val meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend CoroutineScope.() -> T,
) : Data<T>, ComputationGoal<T>(context, dependencies, block)

public class StaticData<T : Any>(
    value: T,
    override val meta: Meta = Meta.EMPTY,
) : Data<T>, StaticGoal<T>(value) {
    override val type: KClass<out T> get() = value::class
}

public class NamedData<out T : Any>(public val name: String, data: Data<T>) : Data<T> by data

public fun <T : Any, R : Any> Data<T>.map(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    block: suspend CoroutineScope.(T) -> R,
): Data<R> = ComputationData(outputType, meta, coroutineContext, listOf(this)) {
    block(await())
}


/**
 * Create a data pipe
 */
public inline fun <T : Any, reified R : Any> Data<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    noinline block: suspend CoroutineScope.(T) -> R,
): Data<R> = ComputationData(R::class, meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Create a joined data.
 */
public inline fun <T : Any, reified R : Any> Collection<Data<T>>.reduce(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    noinline block: suspend CoroutineScope.(Collection<T>) -> R,
): Data<R> = ComputationData(
    R::class,
    meta,
    coroutineContext,
    this
) {
    block(map { run { it.await() } })
}

public fun <K, T : Any, R : Any> Map<K, Data<T>>.reduce(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    block: suspend CoroutineScope.(Map<K, T>) -> R,
): ComputationData<R> = ComputationData(
    outputType,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.await() })
}


/**
 * A joining of multiple data into a single one
 * @param K type of the map key
 * @param T type of the input goal
 * @param R type of the result goal
 */
public inline fun <K, T : Any, reified R : Any> Map<K, Data<T>>.reduce(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    noinline block: suspend CoroutineScope.(Map<K, T>) -> R,
): ComputationData<R> = ComputationData(
    R::class,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.await() })
}


