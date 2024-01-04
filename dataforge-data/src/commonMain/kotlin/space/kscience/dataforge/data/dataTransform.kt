package space.kscience.dataforge.data

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class ValueWithMeta<T>(val meta: Meta, val value: T)

public suspend fun <T : Any> Data<T>.awaitWithMeta(): ValueWithMeta<T> = ValueWithMeta(meta, await())

public data class NamedValueWithMeta<T>(val name: Name, val meta: Meta, val value: T)

public suspend fun <T : Any> NamedData<T>.awaitWithMeta(): NamedValueWithMeta<T> =
    NamedValueWithMeta(name, meta, await())


/**
 * Lazily transform this data to another data. By convention [block] should not use external data (be pure).
 * @param coroutineContext additional [CoroutineContext] elements used for data computation.
 * @param meta for the resulting data. By default equals input data.
 * @param block the transformation itself
 */
public inline fun <T : Any, reified R : Any> Data<T>.map(
    meta: Meta = this.meta,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (T) -> R,
): Data<R> = Data(meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Combine this data with the other data using [block]. See [Data::map] for other details
 */
public inline fun <T1 : Any, T2 : Any, reified R : Any> Data<T1>.combine(
    other: Data<T2>,
    meta: Meta = this.meta,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (left: T1, right: T2) -> R,
): Data<R> = Data(meta, coroutineContext, listOf(this, other)) {
    block(await(), other.await())
}


//data collection operations

@PublishedApi
internal fun Iterable<Data<*>>.joinMeta(): Meta = Meta {
    var counter = 0
    forEach { data ->
        val inputIndex = (data as? NamedData)?.name?.toString() ?: (counter++).toString()
        val token = NameToken("data", inputIndex)
        set(token, data.meta)
    }
}

/**
 * Lazily reduce a collection of [Data] to a single data.
 */
public inline fun <T : Any, reified R : Any> Collection<Data<T>>.reduceToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (List<ValueWithMeta<T>>) -> R,
): Data<R> = Data(
    meta,
    coroutineContext,
    this
) {
    block(map { it.awaitWithMeta() })
}

@PublishedApi
internal fun Map<*, Data<*>>.joinMeta(): Meta = Meta {
    forEach { (key, data) ->
        val token = NameToken("data", key.toString())
        set(token, data.meta)
    }
}

@DFInternal
public fun <K, T : Any, R : Any> Map<K, Data<T>>.reduceToData(
    outputType: KType,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (Map<K, ValueWithMeta<T>>) -> R,
): Data<R> = Data(
    outputType,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.awaitWithMeta() })
}

/**
 * Lazily reduce a [Map] of [Data] with any static key.
 * @param K type of the map key
 * @param T type of the input goal
 * @param R type of the result goal
 */
public inline fun <K, T : Any, reified R : Any> Map<K, Data<T>>.reduceToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Map<K, ValueWithMeta<T>>) -> R,
): Data<R> = Data(
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.awaitWithMeta() })
}

//Iterable operations

@DFInternal
public inline fun <T : Any, R : Any> Iterable<Data<T>>.reduceToData(
    outputType: KType,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Collection<ValueWithMeta<T>>) -> R,
): Data<R> = Data(
    outputType,
    meta,
    coroutineContext,
    toList()
) {
    transformation(map { it.awaitWithMeta() })
}

@OptIn(DFInternal::class)
public inline fun <T : Any, reified R : Any> Iterable<Data<T>>.reduceToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Collection<ValueWithMeta<T>>) -> R,
): Data<R> = reduceToData(typeOf<R>(), meta, coroutineContext) {
    transformation(it)
}

public inline fun <T : Any, reified R : Any> Iterable<Data<T>>.foldToData(
    initial: R,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (result: R, data: ValueWithMeta<T>) -> R,
): Data<R> = reduceToData(
    meta, coroutineContext
) {
    it.fold(initial) { acc, t -> block(acc, t) }
}

/**
 * Transform an [Iterable] of [NamedData] to a single [Data].
 */
@DFInternal
public inline fun <T : Any, R : Any> Iterable<NamedData<T>>.reduceNamedToData(
    outputType: KType,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Collection<NamedValueWithMeta<T>>) -> R,
): Data<R> = Data(
    outputType,
    meta,
    coroutineContext,
    toList()
) {
    transformation(map { it.awaitWithMeta() })
}

@OptIn(DFInternal::class)
public inline fun <T : Any, reified R : Any> Iterable<NamedData<T>>.reduceNamedToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Collection<NamedValueWithMeta<T>>) -> R,
): Data<R> = reduceNamedToData(typeOf<R>(), meta, coroutineContext) {
    transformation(it)
}

/**
 * Fold a [Iterable] of named data into a single [Data]
 */
public inline fun <T : Any, reified R : Any> Iterable<NamedData<T>>.foldNamedToData(
    initial: R,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (result: R, data: NamedValueWithMeta<T>) -> R,
): Data<R> = reduceNamedToData(
    meta, coroutineContext
) {
    it.fold(initial) { acc, t -> block(acc, t) }
}

//DataSet operations

@DFInternal
public suspend fun <T : Any, R : Any> DataSet<T>.map(
    outputType: KType,
    metaTransform: MutableMeta.() -> Unit = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (NamedValueWithMeta<T>) -> R,
): DataTree<R> = DataTree<R>(outputType) {
    forEach {
        val newMeta = it.meta.toMutableMeta().apply(metaTransform).seal()
        val d = Data(outputType, newMeta, coroutineContext, listOf(it)) {
            block(it.awaitWithMeta())
        }
        data(it.name, d)
    }
}

@OptIn(DFInternal::class)
public suspend inline fun <T : Any, reified R : Any> DataSet<T>.map(
    noinline metaTransform: MutableMeta.() -> Unit = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    noinline block: suspend (NamedValueWithMeta<T>) -> R,
): DataTree<R> = map(typeOf<R>(), metaTransform, coroutineContext, block)

public inline fun <T : Any> DataSet<T>.forEach(block: (NamedData<T>) -> Unit) {
    for (d in this) {
        block(d)
    }
}

// DataSet reduction

@PublishedApi
internal fun DataSet<*>.joinMeta(): Meta = Meta {
    forEach { (key, data) ->
        val token = NameToken("data", key.toString())
        set(token, data.meta)
    }
}

public inline fun <T : Any, reified R : Any> DataSet<T>.reduceToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Iterable<NamedValueWithMeta<T>>) -> R,
): Data<R> = asIterable().reduceNamedToData(meta, coroutineContext, transformation)

public inline fun <T : Any, reified R : Any> DataSet<T>.foldToData(
    initial: R,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (result: R, data: NamedValueWithMeta<T>) -> R,
): Data<R> = asIterable().foldNamedToData(initial, meta, coroutineContext, block)