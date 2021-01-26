package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.seal
import hep.dataforge.meta.toMutableMeta
import kotlinx.coroutines.flow.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass


public fun <T : Any, R : Any> Data<T>.map(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    block: suspend (T) -> R,
): Data<R> = LazyData(outputType, meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Create a data mapping
 */
public inline fun <T : Any, reified R : Any> Data<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    crossinline block: suspend (T) -> R,
): Data<R> = LazyData(R::class, meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Combine this data with the other data using [block]
 */
public inline fun <T1 : Any, T2: Any,  reified R : Any> Data<T1>.combine(
    other: Data<T2>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    crossinline block: suspend (left: T1, right: T2) -> R,
): Data<R> = LazyData(R::class, meta, coroutineContext, listOf(this,other)) {
    block(await(), other.await())
}


//data collection operations

/**
 * Create a joined data.
 */
public inline fun <T : Any, reified R : Any> Collection<Data<T>>.reduceToData(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    noinline block: suspend (Collection<T>) -> R,
): Data<R> = LazyData(
    R::class,
    meta,
    coroutineContext,
    this
) {
    block(map { run { it.await() } })
}

public fun <K, T : Any, R : Any> Map<K, Data<T>>.reduceToData(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    block: suspend (Map<K, T>) -> R,
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
public inline fun <K, T : Any, reified R : Any> Map<K, Data<T>>.reduceToData(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    noinline block: suspend (Map<K, T>) -> R,
): LazyData<R> = LazyData(
    R::class,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.await() })
}

/**
 * Transform a [Flow] of [NamedData] to a single [Data]. Execution restrictions are removed for inner [Flow]
 */
public suspend fun <T : Any, R : Any> Flow<NamedData<T>>.reduceToData(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    transformation: suspend (Flow<NamedData<T>>) -> R,
): LazyData<R> = LazyData(
    outputType,
    meta,
    coroutineContext,
    toList()
) {
    transformation(this)
}

//flow operations

public suspend inline fun <T : Any, reified R : Any> Flow<NamedData<T>>.reduceToData(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    noinline transformation: suspend (Flow<NamedData<T>>) -> R,
): LazyData<R> = reduceToData(R::class, coroutineContext, meta) {
    transformation(it)
}

/**
 * Fold a flow of named data into a single [Data]
 */
public suspend inline fun <T : Any, reified R : Any> Flow<NamedData<T>>.foldToData(
    initial: R,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    noinline block: suspend (result: R, data: NamedData<T>) -> R,
): LazyData<R> = reduceToData(
    coroutineContext, meta
) {
    it.fold(initial, block)
}

//DataSet operations

public suspend fun <T : Any, R : Any> DataSet<T>.map(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    metaTransform: MetaBuilder.() -> Unit = {},
    block: suspend (T) -> R,
): DataSet<R> = DataTree(outputType) {
    populate(
        flow().map {
            val newMeta = it.meta.toMutableMeta().apply(metaTransform).seal()
            it.map(outputType, coroutineContext, newMeta, block).named(it.name)
        }
    )
}

public suspend inline fun <T : Any, reified R : Any> DataSet<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    noinline metaTransform: MetaBuilder.() -> Unit = {},
    noinline block: suspend (T) -> R,
): DataSet<R> = map(R::class, coroutineContext, metaTransform, block)

public suspend fun <T : Any> DataSet<T>.forEach(block: suspend (NamedData<T>) -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    flow().collect {
        block(it)
    }
}

public suspend inline fun <T : Any, reified R : Any> DataSet<T>.reduceToData(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    noinline transformation: suspend (Flow<NamedData<T>>) -> R,
): LazyData<R> = flow().reduceToData(coroutineContext, meta, transformation)

public suspend inline fun <T : Any, reified R : Any> DataSet<T>.foldToData(
    initial: R,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    noinline block: suspend (result: R, data: NamedData<T>) -> R,
): LazyData<R> = flow().foldToData(initial, coroutineContext, meta, block)