package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf


private class FlatDataTree<T>(
    override val dataType: KType,
    private val dataSet: Map<Name, Data<T>>,
    private val sourceUpdates: Flow<DataUpdate<T>>,
    private val prefix: Name,
) : DataTree<T> {
    override val data: Data<T>? get() = dataSet[prefix]
    override val items: Map<NameToken, FlatDataTree<T>>
        get() = dataSet.keys
            .filter { it.startsWith(prefix) && it.length > prefix.length }
            .map { it.tokens[prefix.length] }
            .associateWith { FlatDataTree(dataType, dataSet, sourceUpdates, prefix + it) }

    override fun read(name: Name): Data<T>? = dataSet[prefix + name]

    override val updates: Flow<DataUpdate<T>> =
        sourceUpdates.mapNotNull { update ->
            update.name.removeFirstOrNull(prefix)?.let { DataUpdate(dataType, it, update.data) }
        }
}

/**
 * A builder for static [DataTree].
 */
private class DataTreeBuilder<T>(
    private val type: KType,
    initialData: Map<Name, Data<T>> = emptyMap(),
) : DataSink<T> {

    private val map = HashMap<Name, Data<T>>(initialData)

    private val mutex = Mutex()

    private val updatesFlow = MutableSharedFlow<DataUpdate<T>>()

    override fun put(name: Name, data: Data<T>?) {
        if (data == null) {
            map.remove(name)
        } else {
            map[name] = data
        }
    }

    override suspend fun update(name: Name, data: Data<T>?) {
        mutex.withLock {
            if (data == null) {
                map.remove(name)
            } else {
                map.put(name, data)
            }
        }
        updatesFlow.emit(DataUpdate(data?.type ?: type, name, data))
    }

    public fun build(): DataTree<T> = FlatDataTree(type, map, updatesFlow, Name.EMPTY)
}

/**
 * Create a static [DataTree]
 */
@UnsafeKType
public fun <T> DataTree(
    dataType: KType,
    generator: DataSink<T>.() -> Unit,
): DataTree<T> = DataTreeBuilder<T>(dataType).apply(generator).build()

/**
 * Create and a data tree.
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> DataTree(
    noinline generator: DataSink<T>.() -> Unit,
): DataTree<T> = DataTree(typeOf<T>(), generator)


/**
 * Represent this flat data map as a [DataTree] without copying it
 */
@UnsafeKType
public fun <T> Map<Name, Data<T>>.asTree(type: KType): DataTree<T> =
    DataTreeBuilder(type, this).build()

/**
 * Represent this flat data map as a [DataTree] without copying it
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> Map<Name, Data<T>>.asTree(): DataTree<T> = asTree(typeOf<T>())


@UnsafeKType
public fun <T> Sequence<NamedData<T>>.toTree(type: KType): DataTree<T> =
    DataTreeBuilder(type, associate { it.name to it.data }).build()


/**
 * Collect a sequence of [NamedData] to a [DataTree]
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> Sequence<NamedData<T>>.toTree(): DataTree<T> = toTree(typeOf<T>())
