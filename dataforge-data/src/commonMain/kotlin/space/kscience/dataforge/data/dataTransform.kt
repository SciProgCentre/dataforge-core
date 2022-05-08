package space.kscience.dataforge.data

import kotlinx.coroutines.flow.map
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.seal
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class ValueWithMeta<T>(val meta: Meta, val value: T)

public suspend fun <T: Any> Data<T>.awaitWithMeta(): ValueWithMeta<T> = ValueWithMeta(meta, await())

public data class NamedValueWithMeta<T>(val name: Name, val meta: Meta, val value: T)

public suspend fun <T: Any> NamedData<T>.awaitWithMeta(): NamedValueWithMeta<T> = NamedValueWithMeta(name, meta, await())


/**
 * Lazily transform this data to another data. By convention [block] should not use external data (be pure).
 * @param coroutineContext additional [CoroutineContext] elements used for data computation.
 * @param meta for the resulting data. By default equals input data.
 * @param block the transformation itself
 */
public inline fun <T : Any, reified R : Any> Data<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    crossinline block: suspend (T) -> R,
): Data<R> = Data(meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Combine this data with the other data using [block]. See [map] for other details
 */
public inline fun <T1 : Any, T2 : Any, reified R : Any> Data<T1>.combine(
    other: Data<T2>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    crossinline block: suspend (left: T1, right: T2) -> R,
): Data<R> = Data(meta, coroutineContext, listOf(this, other)) {
    block(await(), other.await())
}


//data collection operations

/**
 * Lazily reduce a collection of [Data] to a single data.
 */
public inline fun <T : Any, reified R : Any> Collection<Data<T>>.reduceToData(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline block: suspend (List<ValueWithMeta<T>>) -> R,
): Data<R> = Data(
    meta,
    coroutineContext,
    this
) {
    block(map { it.awaitWithMeta() })
}

@DFInternal
public fun <K, T : Any, R : Any> Map<K, Data<T>>.reduceToData(
    outputType: KType,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
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
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
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
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
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
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline transformation: suspend (Collection<ValueWithMeta<T>>) -> R,
): Data<R> = reduceToData(typeOf<R>(), coroutineContext, meta) {
    transformation(it)
}

public inline fun <T : Any, reified R : Any> Iterable<Data<T>>.foldToData(
    initial: R,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline block: suspend (result: R, data: ValueWithMeta<T>) -> R,
): Data<R> = reduceToData(
    coroutineContext, meta
) {
    it.fold(initial) { acc, t -> block(acc, t) }
}

/**
 * Transform an [Iterable] of [NamedData] to a single [Data].
 */
@DFInternal
public inline fun <T : Any, R : Any> Iterable<NamedData<T>>.reduceNamedToData(
    outputType: KType,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
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
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline transformation: suspend (Collection<NamedValueWithMeta<T>>) -> R,
): Data<R> = reduceNamedToData(typeOf<R>(), coroutineContext, meta) {
    transformation(it)
}

/**
 * Fold a [Iterable] of named data into a single [Data]
 */
public inline fun <T : Any, reified R : Any> Iterable<NamedData<T>>.foldNamedToData(
    initial: R,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline block: suspend (result: R, data: NamedValueWithMeta<T>) -> R,
): Data<R> = reduceNamedToData(
    coroutineContext, meta
) {
    it.fold(initial) { acc, t -> block(acc, t) }
}

//DataSet operations

@DFInternal
public suspend fun <T : Any, R : Any> DataSet<T>.map(
    outputType: KType,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    metaTransform: MutableMeta.() -> Unit = {},
    block: suspend (NamedValueWithMeta<T>) -> R,
): DataTree<R> = DataTree<R>(outputType) {
    populateFrom(
        traverse().map {
            val newMeta = it.meta.toMutableMeta().apply(metaTransform).seal()
            Data(outputType, newMeta, coroutineContext, listOf(it)) {
                block(it.awaitWithMeta())
            }.named(it.name)
        }
    )
}

@OptIn(DFInternal::class)
public suspend inline fun <T : Any, reified R : Any> DataSet<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    noinline metaTransform: MutableMeta.() -> Unit = {},
    noinline block: suspend (NamedValueWithMeta<T>) -> R,
): DataTree<R> = map(typeOf<R>(), coroutineContext, metaTransform, block)

public suspend fun <T : Any> DataSet<T>.forEach(block: suspend (NamedData<T>) -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    traverse().forEach {
        block(it)
    }
}

public inline fun <T : Any, reified R : Any> DataSet<T>.reduceToData(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline transformation: suspend (Iterable<NamedValueWithMeta<T>>) -> R,
): Data<R> = traverse().asIterable().reduceNamedToData(coroutineContext, meta, transformation)

public inline fun <T : Any, reified R : Any> DataSet<T>.foldToData(
    initial: R,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = Meta.EMPTY,
    crossinline block: suspend (result: R, data: NamedValueWithMeta<T>) -> R,
): Data<R> = traverse().asIterable().foldNamedToData(initial, coroutineContext, meta, block)