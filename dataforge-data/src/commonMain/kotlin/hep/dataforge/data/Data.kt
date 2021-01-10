package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.isEmpty
import hep.dataforge.misc.Named
import hep.dataforge.misc.Type
import hep.dataforge.names.Name
import kotlinx.coroutines.*
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

        public fun <T : Any> static(
            value: T,
            meta: Meta = Meta.EMPTY,
        ): Data<T> = StaticData(value, meta)

        /**
         * An empty data containing only meta
         */
        public fun empty(meta: Meta): Data<Nothing> = object : Data<Nothing> {
            override val type: KClass<out Nothing> = Nothing::class
            override val meta: Meta = meta
            override val dependencies: Collection<Goal<*>> = emptyList()
            override val deferred: Deferred<Nothing> get() = GlobalScope.async(start = CoroutineStart.LAZY) {
                error("The Data is empty and could not be computed")
            }
            override fun async(coroutineScope: CoroutineScope): Deferred<Nothing> = deferred
            override fun reset() {}
        }
    }
}

public class LazyData<T : Any>(
    override val type: KClass<out T>,
    override val meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend CoroutineScope.() -> T,
) : Data<T>, LazyGoal<T>(context, dependencies, block)

public class StaticData<T : Any>(
    value: T,
    override val meta: Meta = Meta.EMPTY,
) : Data<T>, StaticGoal<T>(value) {
    override val type: KClass<out T> get() = value::class
}

@Suppress("FunctionName")
public fun <T : Any> Data(
    type: KClass<out T>,
    meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend CoroutineScope.() -> T,
): Data<T> = LazyData(type, meta, context, dependencies, block)

@Suppress("FunctionName")
public inline fun <reified T : Any> Data(
    meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    noinline block: suspend CoroutineScope.() -> T,
): Data<T> = Data(T::class, meta, context, dependencies, block)

public class NamedData<out T : Any> internal constructor(
    override val name: Name,
    public val data: Data<T>,
) : Data<T> by data, Named

public fun <T : Any> Data<T>.named(name: Name): NamedData<T> = if (this is NamedData) {
    NamedData(name, this.data)
} else {
    NamedData(name, this)
}

public fun <T : Any, R : Any> Data<T>.map(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    block: suspend CoroutineScope.(T) -> R,
): Data<R> = LazyData(outputType, meta, coroutineContext, listOf(this)) {
    block(await())
}


/**
 * Create a data pipe
 */
public inline fun <T : Any, reified R : Any> Data<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    noinline block: suspend CoroutineScope.(T) -> R,
): Data<R> = LazyData(R::class, meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Create a joined data.
 */
public inline fun <T : Any, reified R : Any> Collection<Data<T>>.reduce(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    noinline block: suspend CoroutineScope.(Collection<T>) -> R,
): Data<R> = LazyData(
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
): LazyData<R> = LazyData(
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
): LazyData<R> = LazyData(
    R::class,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.await() })
}


