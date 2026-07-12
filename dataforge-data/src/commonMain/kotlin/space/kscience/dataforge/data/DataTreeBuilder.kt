package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A builder for data trees
 */
public interface DataTreeBuilder<in T> : DataBuilderScope<T> {
    public fun put(name: Name, data: Data<T>)

    /**
     * Asynchronously update the data tree with corresponding update [scope].
     * When the [scope] is cancelled, the update is cancelled as well.
     *
     * This method could be called multiple times. In this case different updaters are applied simultaneously and concurrently.
     *
     * Since updates are concurrent, the specific order of application is undetermined.
     */
    public fun update(scope: CoroutineScope, block: suspend DataSink<T>.() -> Unit)
}

/**
 * Put root data into the builder
 */
public fun <T> DataTreeBuilder<T>.data(data: Data<T>): Unit = put(Name.EMPTY, data)

/**
 * Map-based implementation of [DataTree]
 */
private class FlatDataTree<T>(
    override val dataType: KType,
    private val dataSet: Map<Name, Data<T>>,
    private val sourceUpdates: SharedFlow<Name>,
    private val prefix: Name,
) : DataTree<T> {
    override val data: Data<T>? get() = dataSet[prefix]
    override val items: Map<NameToken, FlatDataTree<T>>
        get() = dataSet.keys
            .filter { it.startsWith(prefix) && it.length > prefix.length }
            .map { it.tokens[prefix.length] }
            .associateWith { FlatDataTree(dataType, dataSet, sourceUpdates, prefix + it) }

    override fun read(name: Name): Data<T>? = dataSet[prefix + name]

    override val updates: Flow<Name> = sourceUpdates.mapNotNull { update ->
        update.removeFirstOrNull(prefix)
    }
}

/**
 * A builder for [FlatDataTree].
 */
private class DataTreeBuilderImpl<T>(
    private val type: KType,
    initialData: Map<Name, Data<T>> = emptyMap(),
) : DataSink<T>, DataTreeBuilder<T> {

    private val map = HashMap<Name, Data<T>>(initialData)

    private val mutex = Mutex()

    private val updatesFlow = MutableSharedFlow<Name>()


    override fun put(name: Name, data: Data<T>) {
        map[name] = data
    }

    override suspend fun write(name: Name, data: Data<T>?) {
        mutex.withLock {
            if (data == null) {
                map.remove(name)
            } else {
                map[name] = data
            }
        }
        updatesFlow.emit(name)
    }

    override fun update(
        scope: CoroutineScope,
        block: suspend DataSink<T>.() -> Unit
    ) {
        scope.launch { block() }
    }

    fun build(): DataTree<T> = FlatDataTree(type, map, updatesFlow, Name.EMPTY)
}

private val emptySharedFlow = MutableSharedFlow<Nothing>()


public fun <T> DataTreeBuilder<T>.put(name: String, data: Data<T>) {
    put(name.parseAsName(), data)
}

/**
 * Put static value with given [name]
 */
public inline fun <T, reified T1 : T> DataTreeBuilder<T>.putValue(
    name: String,
    value: T1,
    meta: Meta = Meta.EMPTY
) {
    put(name, Data(value, meta))
}

/**
 * Put static value as root data.
 */
public inline fun <T, reified T1 : T> DataTreeBuilder<T>.value(
    value: T1,
    meta: Meta = Meta.EMPTY
) {
    put(Name.EMPTY, Data(value, meta))
}


/**
 * Put a node using provided builder [block]
 */
public fun <T> DataTreeBuilder<T>.node(prefix: Name, block: DataTreeBuilder<T>.() -> Unit) {
    if(prefix.isEmpty()) return block()

    val proxy = object : DataTreeBuilder<T> {
        override fun put(name: Name, data: Data<T>) {
            this@node.put(prefix + name, data)
        }

        override fun update(
            scope: CoroutineScope,
            block: suspend DataSink<T>.() -> Unit
        ) {
            this@node.update(scope){
                val sinkProxy = DataSink<T> { name, data -> write(prefix + name, data) }
                sinkProxy.block()
            }
        }

    }
    return proxy.block()
}

public fun <T> DataTreeBuilder<T>.node(prefix: String, block: DataTreeBuilder<T>.() -> Unit): Unit =
    node(prefix.parseAsName(), block)

/**
 * Put a static [DataTree] with given prefix.
 */
public fun <T> DataTreeBuilder<T>.node(prefix: Name, tree: DataTree<T>) {
    tree.forEach { data ->
        put(prefix + data.name, data)
    }
}

/**
 * Write current state of the [tree] into this builder. Does not propagate updates from it.
 */
public fun <T> DataTreeBuilder<T>.node(prefix: String, tree: DataTree<T>): Unit = node(prefix.parseAsName(), tree)

/**
 * Write current state of the [tree] into this builder and propagate updates from it.
 */
public fun <T> DataTreeBuilder<T>.observeNode(prefix: Name, scope: CoroutineScope, tree: DataTree<T>) {
    node(prefix, tree)

    update(scope) {
        tree.updates.collect {
            write(prefix + it, tree[it])
        }
    }
}

/**
 * Write current state of the [tree] into this builder and propagate updates from it.
 */
public fun <T> DataTreeBuilder<T>.observeNode(prefix: String, scope: CoroutineScope, tree: DataTree<T>): Unit =
    observeNode(prefix.parseAsName(), scope, tree)

/**
 * Create a static [DataTree] from a flat map
 */
@UnsafeKType
public fun <T> DataTree(type: KType, data: Map<Name, Data<T>>): DataTree<T> =
    DataTreeBuilderImpl(type, data).build()

/**
 * Create a dynamic [DataTree]
 */
@UnsafeKType
public fun <T> DataTree(
    type: KType,
    block: DataTreeBuilder<T>.() -> Unit
): DataTree<T> {

    val initialData = mutableMapOf<Name, Data<T>>()
    val updaters = mutableListOf<Pair<CoroutineScope, suspend DataSink<T>.() -> Unit>>()

    val dynamicDataBuilder = object : DataTreeBuilder<T> {
        override fun update(scope: CoroutineScope, block: suspend DataSink<T>.() -> Unit) {
            updaters.add(scope to block)
        }

        override fun put(name: Name, data: Data<T>) {
            initialData[name] = data
        }

    }

    dynamicDataBuilder.block()

    return if (updaters.isEmpty()) {
        FlatDataTree(type, initialData, emptySharedFlow, Name.EMPTY)
    } else {
        DataTreeBuilderImpl<T>(type, initialData).apply {
            updaters.forEach { (scope, updater) ->
                scope.launch(GoalExecutionRestriction(GoalExecutionRestrictionPolicy.ERROR)) {
                    updater()
                }
            }
        }.build()
    }
}

/**
 * Creates a dynamic [DataTree] of the specified type.
 *
 * This method constructs a [DataTree] where updates to the data are applied
 * dynamically and concurrently. The specific order of update application
 * is undetermined due to the concurrent nature of operations.
 *
 * @param T The type of the data encapsulated in the [DataTree].
 * @param block A configuration block defining the dynamic data updates and initial data structure.
 * @return A dynamic [DataTree] instance of type [T].
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> DataTree(
    noinline block: DataTreeBuilder<T>.() -> Unit
): DataTree<T> = DataTree(typeOf<T>(), block)

/**
 * Create a static [DataTree] from a flat map
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> DataTree(
    data: Map<Name, Data<T>>
): DataTree<T> = DataTree(typeOf<T>(), data)

/**
 * Represent this flat data map as a [DataTree] without copying it
 */
@UnsafeKType
public fun <T> Map<Name, Data<T>>.asTree(type: KType): DataTree<T> = FlatDataTree(
    dataType = type,
    dataSet = this,
    sourceUpdates = emptySharedFlow,
    prefix = Name.EMPTY
)

/**
 * Represent this flat data map as a [DataTree] without copying it
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> Map<Name, Data<T>>.asTree(): DataTree<T> = asTree(typeOf<T>())


/**
 * Collect a sequence of [NamedData] to a [DataTree]
 */
@UnsafeKType
public fun <T> Sequence<NamedData<T>>.toTree(type: KType): DataTree<T> = FlatDataTree(
    dataType = type,
    dataSet = associateBy { it.name },
    sourceUpdates = emptySharedFlow,
    prefix = Name.EMPTY
)


/**
 * Collect a sequence of [NamedData] to a [DataTree]
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> Sequence<NamedData<T>>.toTree(): DataTree<T> = toTree(typeOf<T>())