package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class ValueWithMeta<T>(val value: T, val meta: Meta)

public suspend fun <T> Data<T>.awaitWithMeta(): ValueWithMeta<T> = ValueWithMeta(await(), meta)

public data class NamedValueWithMeta<T>(val name: Name, val value: T, val meta: Meta)

public suspend fun <T> NamedData<T>.awaitWithMeta(): NamedValueWithMeta<T> =
    NamedValueWithMeta(name, await(), meta)

/**
 * Lazily transform this data to another data. By convention [block] should not use external data (be pure).
 * @param type explicit type of the resulting [Data]
 * @param coroutineContext additional [CoroutineContext] elements used for data computation.
 * @param meta for the resulting data. By default equals input data.
 * @param block the transformation itself
 */
@UnsafeKType
public fun <T, R> Data<T>.transform(
    type: KType,
    meta: Meta = this.meta,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (T) -> R,
): Data<R> = Data(type, meta, coroutineContext, listOf(this)) {
    block(await())
}


/**
 * Lazily transform this data to another data. By convention [block] should not use external data (be pure).
 * @param coroutineContext additional [CoroutineContext] elements used for data computation.
 * @param meta for the resulting data. By default equals input data.
 * @param block the transformation itself
 */
public inline fun <T, reified R> Data<T>.transform(
    meta: Meta = this.meta,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (T) -> R,
): Data<R> = Data(meta, coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Combine this data with the other data using [block]. See [Data::map] for other details
 */
public inline fun <T1, T2, reified R> Data<T1>.combine(
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


@PublishedApi
internal fun Map<*, Data<*>>.joinMeta(): Meta = Meta {
    forEach { (key, data) ->
        val token = NameToken("data", key.toString())
        set(token, data.meta)
    }
}

@UnsafeKType
public fun <K, T, R> Map<K, Data<T>>.reduceToData(
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
public inline fun <K, T, reified R> Map<K, Data<T>>.reduceToData(
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

@UnsafeKType
public inline fun <T, R> Iterable<Data<T>>.reduceToData(
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

@OptIn(UnsafeKType::class)
public inline fun <T, reified R> Iterable<Data<T>>.reduceToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Collection<ValueWithMeta<T>>) -> R,
): Data<R> = reduceToData(typeOf<R>(), meta, coroutineContext) {
    transformation(it)
}

public inline fun <T, reified R> Iterable<Data<T>>.foldToData(
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
@UnsafeKType
public inline fun <T, R> Iterable<NamedData<T>>.reduceNamedToData(
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

@OptIn(UnsafeKType::class)
public inline fun <T, reified R> Iterable<NamedData<T>>.reduceNamedToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Collection<NamedValueWithMeta<T>>) -> R,
): Data<R> = reduceNamedToData(typeOf<R>(), meta, coroutineContext) {
    transformation(it)
}

/**
 * Fold a [Iterable] of named data into a single [Data]
 */
public inline fun <T, reified R> Iterable<NamedData<T>>.foldNamedToData(
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


@UnsafeKType
public fun <T, R> DataTree<T>.transformEach(
    outputType: KType,
    scope: CoroutineScope,
    metaTransform: MutableMeta.(name: Name) -> Unit = {},
    compute: suspend (NamedValueWithMeta<T>) -> R,
): DataTree<R> = DataTree.dynamic<R>(
    outputType,
    scope
) {
    asSequence().forEach { namedData: NamedData<T> ->
        val newMeta = namedData.meta.toMutableMeta().apply {
            metaTransform(namedData.name)
        }.seal()
        val newData = Data(outputType, newMeta, scope.coroutineContext, listOf(namedData)) {
            compute(namedData.awaitWithMeta())
        }
        data(namedData.name, newData)
    }
    update {
        updates.collect { name ->
            val data: Data<T>? = read(name)
            if (data == null) write(name, null) else {
                val newMeta = data.meta.toMutableMeta().apply {
                    metaTransform(name)
                }.seal()
                val d = Data(outputType, newMeta, scope.coroutineContext, listOf(data)) {
                    compute(NamedValueWithMeta(name, data.await(), data.meta))
                }
                write(name, d)
            }
        }
    }
}

@OptIn(UnsafeKType::class)
public inline fun <T, reified R> DataTree<T>.transformEach(
    scope: CoroutineScope,
    noinline metaTransform: MutableMeta.(name: Name) -> Unit = {},
    noinline block: suspend (NamedValueWithMeta<T>) -> R,
): DataTree<R> = transformEach(typeOf<R>(), scope, metaTransform, block)

public inline fun <T> DataTree<T>.forEach(block: (NamedData<T>) -> Unit) {
    asSequence().forEach(block)
}

// DataSet snapshot reduction

@PublishedApi
internal fun DataTree<*>.joinMeta(): Meta = Meta {
    asSequence().forEach {
        val token = NameToken("data", it.name.toString())
        set(token, it.meta)
    }
}

/**
 * Reduce current snapshot of the [DataTree] to a single [Data].
 * Even if a tree is changed in the future, only the current data set is taken.
 */
public inline fun <T, reified R> DataTree<T>.reduceToData(
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transformation: suspend (Iterable<NamedValueWithMeta<T>>) -> R,
): Data<R> = asSequence().asIterable().reduceNamedToData(meta, coroutineContext, transformation)

public inline fun <T, reified R> DataTree<T>.foldToData(
    initial: R,
    meta: Meta = joinMeta(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (result: R, data: NamedValueWithMeta<T>) -> R,
): Data<R> = asSequence().asIterable().foldNamedToData(initial, meta, coroutineContext, block)