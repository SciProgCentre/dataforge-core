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
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A builder for data trees
 */
public fun interface DataBuilder<T> : DataBuilderScope<T> {
    public fun data(name: Name, data: Data<T>)
}

@Deprecated("Use DataBuilder instead", ReplaceWith("DataBuilder<T>"))
public typealias StaticDataBuilder<T> = DataBuilder<T>

/**
 * A builder for dynamic data trees
 */
public interface DynamicDataBuilder<T> : DataBuilder<T> {
    /**
     * Asynchronously update the data tree.
     *
     * This method could be called multiple times. In this case different updaters are applied simultaneously and concurrently.
     *
     * Since updates are concurrent, the specific order of application is undetermined.
     */
    public fun update(block: suspend DataSink<T>.() -> Unit)
}

/**
 * A builder for data tree branches and static data
 */
private class DataMapBuilder<T> : DataBuilder<T> {
    val map = mutableMapOf<Name, Data<T>>()

    override fun data(name: Name, data: Data<T>) {
        if (map.containsKey(name)) {
            error("Duplicate key '$name'")
        } else {
            map[name] = data
        }
    }
}

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
private class DataTreeBuilder<T>(
    private val type: KType,
    initialData: Map<Name, Data<T>> = emptyMap(),
) : DataSink<T> {

    private val map = HashMap<Name, Data<T>>(initialData)

    private val mutex = Mutex()

    private val updatesFlow = MutableSharedFlow<Name>()


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

    fun build(): DataTree<T> = FlatDataTree(type, map, updatesFlow, Name.EMPTY)
}

private val emptySharedFlow = MutableSharedFlow<Nothing>()


public fun <T> DataBuilder<T>.data(name: String, data: Data<T>) {
    data(name.parseAsName(), data)
}

public inline fun <T, reified T1 : T> DataBuilder<T>.value(
    name: String,
    value: T1,
    metaBuilder: MutableMeta.() -> Unit = {}
) {
    data(name, Data(value, Meta(metaBuilder)))
}

public fun <T> DataBuilder<T>.node(prefix: Name, block: DataBuilder<T>.() -> Unit) {
    val map = DataMapBuilder<T>().apply(block).map
    map.forEach { (name, data) ->
        data(prefix + name, data)
    }
}

public fun <T> DataBuilder<T>.node(prefix: String, block: DataBuilder<T>.() -> Unit): Unit =
    node(prefix.parseAsName(), block)

public fun <T> DataBuilder<T>.node(prefix: Name, tree: DataTree<T>) {
    tree.forEach { data ->
        data(prefix + data.name, data)
    }
}

/**
 * Write current state of the [tree] into this builder. Does not propagate updates from it.
 */
public fun <T> DataBuilder<T>.node(prefix: String, tree: DataTree<T>): Unit = node(prefix.parseAsName(), tree)

/**
 * Write current state of the [tree] into this builder and propagate updates from it.
 */
public fun <T> DynamicDataBuilder<T>.observeNode(prefix: Name, tree: DataTree<T>) {
    node(prefix, tree)

    update {
        tree.updates.collect {
            write(prefix + it, tree[it])
        }
    }
}

public fun <T> DynamicDataBuilder<T>.observeNode(prefix: String, tree: DataTree<T>): Unit =
    observeNode(prefix.parseAsName(), tree)

/**
 * Create a static [DataTree] from a flat map
 */
@UnsafeKType
public fun <T> DataTree(type: KType, data: Map<Name, Data<T>>): DataTree<T> =
    DataTreeBuilder(type, data).build()

/**
 * Create a dynamic [DataTree]
 */
public fun <T> DataTree.Companion.dynamic(
    type: KType,
    scope: CoroutineScope,
    block: DynamicDataBuilder<T>.() -> Unit
): DataTree<T> {

    val initialData = mutableMapOf<Name, Data<T>>()
    val updaters = mutableListOf<suspend DataSink<T>.() -> Unit>()

    val dynamicDataBuilder = object : DynamicDataBuilder<T> {
        override fun update(block: suspend DataSink<T>.() -> Unit) {
            updaters.add(block)
        }

        override fun data(name: Name, data: Data<T>) {
            initialData[name] = data
        }

    }

    dynamicDataBuilder.block()

    return if (updaters.isEmpty()) {
        FlatDataTree(type, initialData, emptySharedFlow, Name.EMPTY)
    } else {
        DataTreeBuilder<T>(type, initialData).apply {
            updaters.forEach { updater ->
                scope.launch(GoalExecutionRestriction(GoalExecutionRestrictionPolicy.ERROR)) {
                    updater()
                }
            }
        }.build()
    }
}


@OptIn(UnsafeKType::class)
public inline fun <reified T> DataTree.Companion.dynamic(
    scope: CoroutineScope,
    noinline block: DynamicDataBuilder<T>.() -> Unit
): DataTree<T> = dynamic(typeOf<T>(), scope, block)

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


@UnsafeKType
public fun <T> DataTree.Companion.static(
    type: KType, block: DataBuilder<T>.() -> Unit
): DataTree<T> = DataMapBuilder<T>().apply(block).map.asTree(type)


@OptIn(UnsafeKType::class)
public inline fun <reified T> DataTree.Companion.static(
    noinline block: DataBuilder<T>.() -> Unit
): DataTree<T> = static(typeOf<T>(), block)