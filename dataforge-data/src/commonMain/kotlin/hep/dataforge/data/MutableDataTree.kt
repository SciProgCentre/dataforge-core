package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * A mutable [DataTree.Companion.dynamic]. It
 */
public class MutableDataTree<T : Any>(
    override val dataType: KClass<out T>,
    public val scope: CoroutineScope,
) : DataTree<T>, DataSetBuilder<T> {
    private val mutex = Mutex()
    private val treeItems = HashMap<NameToken, DataTreeItem<T>>()

    override suspend fun items(): Map<NameToken, DataTreeItem<T>> = mutex.withLock { treeItems }

    private val _updates = MutableSharedFlow<Name>()

    override val updates: Flow<Name>
        get() = _updates

    private suspend fun remove(token: NameToken) {
        mutex.withLock {
            if (treeItems.remove(token) != null) {
                _updates.emit(token.asName())
            }
        }
    }

    override fun remove(name: Name) {
        scope.launch {
            if (name.isEmpty()) error("Can't remove the root node")
            (getItem(name.cutLast()).tree as? MutableDataTree)?.remove(name.lastOrNull()!!)
        }
    }

    private suspend fun set(token: NameToken, node: DataSet<T>) {
        //if (_map.containsKey(token)) error("Tree entry with name $token is not empty")
        mutex.withLock {
            treeItems[token] = DataTreeItem.Node(node.toMutableTree(scope))
            coroutineScope {
                node.updates.onEach {
                    _updates.emit(token + it)
                }.launchIn(this)
            }
            _updates.emit(token.asName())
        }
    }

    private suspend fun set(token: NameToken, data: Data<T>) {
        mutex.withLock {
            treeItems[token] = DataTreeItem.Leaf(data)
            _updates.emit(token.asName())
        }
    }

    private suspend fun getOrCreateNode(token: NameToken): MutableDataTree<T> =
        (treeItems[token] as? DataTreeItem.Node<T>)?.tree as? MutableDataTree<T>
            ?: MutableDataTree(dataType, scope).also { set(token, it) }

    private suspend fun getOrCreateNode(name: Name): MutableDataTree<T> {
        return when (name.length) {
            0 -> this
            1 -> getOrCreateNode(name.firstOrNull()!!)
            else -> getOrCreateNode(name.firstOrNull()!!).getOrCreateNode(name.cutFirst())
        }
    }

    override fun set(name: Name, data: Data<T>?) {
        if (data == null) {
            remove(name)
        } else {
            scope.launch {
                when (name.length) {
                    0 -> error("Can't add data with empty name")
                    1 -> set(name.firstOrNull()!!, data)
                    2 -> getOrCreateNode(name.cutLast()).set(name.lastOrNull()!!, data)
                }
            }
        }
    }

    private suspend fun setTree(name: Name, node: MutableDataTree<out T>) {
        when (name.length) {
            0 -> error("Can't add data with empty name")
            1 -> set(name.firstOrNull()!!, node)
            2 -> getOrCreateNode(name.cutLast()).set(name.lastOrNull()!!, node)
        }
    }

    override suspend fun set(name: Name, dataSet: DataSet<T>): Unit {
        if (dataSet is MutableDataTree) {
            setTree(name, dataSet)
        } else {
            setTree(name, dataSet.toMutableTree(scope))
        }
    }

    override fun set(name: Name, block: DataSetBuilder<T>.() -> Unit) {
        scope.launch {
            setTree(name, MutableDataTree(dataType, scope).apply(block))
        }
    }

    public fun collectFrom(flow: Flow<NamedData<T>>) {
        flow.onEach {
            set(it.name, it.data)
        }.launchIn(scope)
    }
}

public suspend fun <T : Any> DataTree.Companion.dynamic(
    type: KClass<out T>,
    updatesScope: CoroutineScope,
    block: suspend MutableDataTree<T>.() -> Unit,
): DataTree<T> {
    val tree = MutableDataTree(type, updatesScope)
    tree.block()
    return tree
}

public suspend inline fun <reified T : Any> DataTree.Companion.dynamic(
    updatesScope: CoroutineScope,
    crossinline block: suspend MutableDataTree<T>.() -> Unit,
): DataTree<T> = MutableDataTree(T::class, updatesScope).apply { block() }


public suspend inline fun <reified T : Any> MutableDataTree<T>.set(
    name: Name,
    noinline block: suspend MutableDataTree<T>.() -> Unit,
): Unit = set(name, DataTree.dynamic(T::class, scope, block))

public suspend inline fun <reified T : Any> MutableDataTree<T>.set(
    name: String,
    noinline block: suspend MutableDataTree<T>.() -> Unit,
): Unit = set(name.toName(), DataTree.dynamic(T::class, scope, block))

/**
 * Generate a mutable builder from this node. Node content is not changed
 */
public suspend fun <T : Any> DataSet<T>.toMutableTree(
    scope: CoroutineScope,
): MutableDataTree<T> = MutableDataTree(dataType, scope).apply {
    flow().collect { set(it.name, it.data) }
    this@toMutableTree.updates.onEach {
        set(it, getData(it))
    }.launchIn(scope)
}
