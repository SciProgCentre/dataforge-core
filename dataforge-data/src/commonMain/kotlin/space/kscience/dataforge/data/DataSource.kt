package space.kscience.dataforge.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.*
import kotlin.contracts.contract
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A generic data provider
 */
public interface DataSource<out T> {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val dataType: KType

    /**
     * Get data with given name. Or null if it is not present
     */
    public fun read(name: Name): Data<T>?
}

/**
 * A data provider with possible dynamic updates
 */
public interface ObservableDataSource<out T> : DataSource<T> {

    /**
     * Flow updates made to the data
     */
    public fun updates(): Flow<NamedData<T>>
}

/**
 * A tree like structure for data holding
 */
public interface GenericDataTree<out T, out TR : GenericDataTree<T, TR>> : DataSource<T> {
    public val self: TR

    public val data: Data<T>?
    public val items: Map<NameToken, TR>


    override fun read(name: Name): Data<T>? = when (name.length) {
        0 -> data
        else -> items[name.first()]?.read(name.cutFirst())
    }

    public companion object {
        private object EmptyDataTree : GenericDataTree<Nothing, EmptyDataTree> {
            override val self: EmptyDataTree get() = this
            override val data: Data<Nothing>? = null
            override val items: Map<NameToken, EmptyDataTree> = emptyMap()
            override val dataType: KType = typeOf<Unit>()

            override fun read(name: Name): Data<Nothing>? = null

        }

        public val EMPTY: GenericDataTree<Nothing, *> = EmptyDataTree
    }
}

public typealias DataTree<T> = GenericDataTree<T, GenericDataTree<T, *>>

/**
 * Return a single data in this tree. Throw error if it is not single.
 */
public fun <T> DataTree<T>.single(): NamedData<T> = asSequence().single()

/**
 * An alias for easier access to tree values
 */
public operator fun <T> DataTree<T>.get(name: Name): Data<T>? = read(name)

public operator fun <T> DataTree<T>.get(name: String): Data<T>? = read(name.parseAsName())

/**
 * Return a sequence of all data items in this tree.
 * This method does not take updates into account.
 */
public fun <T> DataTree<T>.asSequence(
    namePrefix: Name = Name.EMPTY,
): Sequence<NamedData<T>> = sequence {
    data?.let { yield(it.named(namePrefix)) }
    items.forEach { (token, tree) ->
        yieldAll(tree.asSequence(namePrefix + token))
    }
}

public val DataTree<*>.meta: Meta? get() = data?.meta

/**
 * Provide subtree if it exists
 */
public tailrec fun <T, TR : GenericDataTree<T, TR>> GenericDataTree<T, TR>.putAll(name: Name): TR? =
    when (name.length) {
        0 -> self
        1 -> items[name.first()]
        else -> items[name.first()]?.putAll(name.cutFirst())
    }

public fun <T, TR : GenericDataTree<T, TR>> GenericDataTree<T, TR>.putAll(name: String): TR? =
    this@branch.putAll(name.parseAsName())

public fun GenericDataTree<*, *>.isEmpty(): Boolean = data == null && items.isEmpty()

@PublishedApi
internal class FlatDataTree<T>(
    override val dataType: KType,
    private val dataSet: Map<Name, Data<T>>,
    private val prefix: Name,
) : GenericDataTree<T, FlatDataTree<T>> {
    override val self: FlatDataTree<T> get() = this
    override val data: Data<T>? get() = dataSet[prefix]
    override val items: Map<NameToken, FlatDataTree<T>>
        get() = dataSet.keys
            .filter { it.startsWith(prefix) && it.length > prefix.length }
            .map { it.tokens[prefix.length] }
            .associateWith { FlatDataTree(dataType, dataSet, prefix + it) }

    override fun read(name: Name): Data<T>? = dataSet[prefix + name]
}

/**
 * Represent this flat data map as a [DataTree] without copying it
 */
public inline fun <reified T> Map<Name, Data<T>>.asTree(): DataTree<T> = FlatDataTree(typeOf<T>(), this, Name.EMPTY)

internal fun <T> Sequence<NamedData<T>>.toTree(type: KType): DataTree<T> =
    FlatDataTree(type, associate { it.name to it.data }, Name.EMPTY)

/**
 * Collect a sequence of [NamedData] to a [DataTree]
 */
public inline fun <reified T> Sequence<NamedData<T>>.toTree(): DataTree<T> =
    FlatDataTree(typeOf<T>(), associate { it.name to it.data }, Name.EMPTY)

public interface GenericObservableDataTree<out T, out TR : GenericObservableDataTree<T, TR>> :
    GenericDataTree<T, TR>, ObservableDataSource<T>, AutoCloseable {

    /**
     * A scope that is used to propagate updates. When this scope is closed, no new updates could arrive.
     */
    public val updatesScope: CoroutineScope

    /**
     * Close this data tree updates channel
     */
    override fun close() {
        updatesScope.cancel()
    }

}

public typealias ObservableDataTree<T> = GenericObservableDataTree<T, GenericObservableDataTree<T, *>>

/**
 * Check if the [DataTree] is observable
 */
public fun <T> DataTree<T>.isObservable(): Boolean {
    contract {
        returns(true) implies (this@isObservable is GenericObservableDataTree<T, *>)
    }
    return this is GenericObservableDataTree<T, *>
}

/**
 * Wait for this data tree to stop spawning updates (updatesScope is closed).
 * If this [DataTree] is not observable, return immediately.
 */
public suspend fun <T> DataTree<T>.awaitClose() {
    if (isObservable()) {
        updatesScope.coroutineContext[Job]?.join()
    }
}

public fun <T> DataTree<T>.updates(): Flow<NamedData<T>> =
    if (this is GenericObservableDataTree<T, *>) updates() else emptyFlow()

public fun interface DataSink<in T> {
    public fun put(name: Name, data: Data<T>?)
}

@DFInternal
public class DataTreeBuilder<T>(private val type: KType) : DataSink<T> {
    private val map = HashMap<Name, Data<T>>()
    override fun put(name: Name, data: Data<T>?) {
        if (data == null) {
            map.remove(name)
        } else {
            map[name] = data
        }
    }

    public fun build(): DataTree<T> = FlatDataTree(type, map, Name.EMPTY)
}

@DFInternal
public inline fun <T> DataTree(
    dataType: KType,
    generator: DataSink<T>.() -> Unit,
): DataTree<T> = DataTreeBuilder<T>(dataType).apply(generator).build()

/**
 * Create and a data tree.
 */
@OptIn(DFInternal::class)
public inline fun <reified T> DataTree(
    generator: DataSink<T>.() -> Unit,
): DataTree<T> = DataTreeBuilder<T>(typeOf<T>()).apply(generator).build()

/**
 * A mutable version of [GenericDataTree]
 */
public interface MutableDataTree<T> : GenericObservableDataTree<T, MutableDataTree<T>>, DataSink<T> {
    override var data: Data<T>?

    override val items: Map<NameToken, MutableDataTree<T>>

    public fun getOrCreateItem(token: NameToken): MutableDataTree<T>

    public operator fun set(token: NameToken, data: Data<T>?)

    override fun put(name: Name, data: Data<T>?): Unit = set(name, data)
}

public tailrec operator fun <T> MutableDataTree<T>.set(name: Name, data: Data<T>?): Unit {
    when (name.length) {
        0 -> this.data = data
        1 -> set(name.first(), data)
        else -> getOrCreateItem(name.first())[name.cutFirst()] = data
    }
}

private class MutableDataTreeImpl<T>(
    override val dataType: KType,
    override val updatesScope: CoroutineScope,
) : MutableDataTree<T> {


    private val updates = MutableSharedFlow<NamedData<T>>()

    private val children = HashMap<NameToken, MutableDataTree<T>>()

    override var data: Data<T>? = null
        set(value) {
            if (!updatesScope.isActive) error("Can't send updates to closed MutableDataTree")
            field = value
            if (value != null) {
                updatesScope.launch {
                    updates.emit(value.named(Name.EMPTY))
                }
            }
        }

    override val items: Map<NameToken, MutableDataTree<T>> get() = children

    override fun getOrCreateItem(token: NameToken): MutableDataTree<T> = children.getOrPut(token){
        MutableDataTreeImpl(dataType, updatesScope)
    }

    override val self: MutableDataTree<T> get() = this

    override fun set(token: NameToken, data: Data<T>?) {
        if (!updatesScope.isActive) error("Can't send updates to closed MutableDataTree")
        val subTree = getOrCreateItem(token)
        subTree.updates().onEach {
            updates.emit(it.named(token + it.name))
        }.launchIn(updatesScope)
        subTree.data = data
    }

    override fun updates(): Flow<NamedData<T>> = updates
}

/**
 * Create a new [MutableDataTree]
 *
 * @param parentScope a [CoroutineScope] to control data propagation. By default uses [GlobalScope]
 */
@OptIn(DelicateCoroutinesApi::class)
public fun <T> MutableDataTree(
    type: KType,
    parentScope: CoroutineScope = GlobalScope,
): MutableDataTree<T> = MutableDataTreeImpl<T>(
    type,
    CoroutineScope(parentScope.coroutineContext + Job(parentScope.coroutineContext[Job]))
)

/**
 * Create and initialize a observable mutable data tree.
 */
@OptIn(DelicateCoroutinesApi::class)
public inline fun <reified T> MutableDataTree(
    parentScope: CoroutineScope = GlobalScope,
    generator: MutableDataTree<T>.() -> Unit = {},
): MutableDataTree<T> = MutableDataTree<T>(typeOf<T>(), parentScope).apply { generator() }

//@DFInternal
//public fun <T> ObservableDataTree(
//    type: KType,
//    scope: CoroutineScope,
//    generator: suspend MutableDataTree<T>.() -> Unit = {},
//): ObservableDataTree<T> = MutableDataTree<T>(type, scope.coroutineContext).apply(generator)

public inline fun <reified T> ObservableDataTree(
    parentScope: CoroutineScope,
    generator: MutableDataTree<T>.() -> Unit = {},
): ObservableDataTree<T> = MutableDataTree<T>(typeOf<T>(), parentScope).apply(generator)


/**
 * Collect a [Sequence] into an observable tree with additional [updates]
 */
public fun <T> Sequence<NamedData<T>>.toObservableTree(
    dataType: KType,
    parentScope: CoroutineScope,
    updates: Flow<NamedData<T>>,
): ObservableDataTree<T> = MutableDataTree<T>(dataType, parentScope).apply {
    this.putAll(this@toObservableTree)
    updates.onEach {
        put(it.name, it.data)
    }.launchIn(updatesScope)
}
