package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * A mutable [DataTree.Companion.active]. It
 */
public class ActiveDataTree<T : Any>(
    override val dataType: KClass<out T>,
) : DataTree<T>, DataSetBuilder<T>, ActiveDataSet<T> {
    private val mutex = Mutex()
    private val treeItems = HashMap<NameToken, DataTreeItem<T>>()

    override suspend fun items(): Map<NameToken, DataTreeItem<T>> = mutex.withLock {
        treeItems.filter { !it.key.body.startsWith("@") }
    }

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

    override suspend fun remove(name: Name) {
        if (name.isEmpty()) error("Can't remove the root node")
        (getItem(name.cutLast()).tree as? ActiveDataTree)?.remove(name.lastOrNull()!!)
    }

    private suspend fun set(token: NameToken, data: Data<T>) {
        mutex.withLock {
            treeItems[token] = DataTreeItem.Leaf(data)
        }
    }

    private suspend fun getOrCreateNode(token: NameToken): ActiveDataTree<T> =
        (treeItems[token] as? DataTreeItem.Node<T>)?.tree as? ActiveDataTree<T>
            ?: ActiveDataTree(dataType).also {
                mutex.withLock {
                    treeItems[token] = DataTreeItem.Node(it)
                }
            }

    private suspend fun getOrCreateNode(name: Name): ActiveDataTree<T> {
        return when (name.length) {
            0 -> this
            1 -> getOrCreateNode(name.firstOrNull()!!)
            else -> getOrCreateNode(name.firstOrNull()!!).getOrCreateNode(name.cutFirst())
        }
    }

    override suspend fun emit(name: Name, data: Data<T>?) {
        if (data == null) {
            remove(name)
        } else {
            when (name.length) {
                0 -> error("Can't add data with empty name")
                1 -> set(name.firstOrNull()!!, data)
                2 -> getOrCreateNode(name.cutLast()).set(name.lastOrNull()!!, data)
            }
        }
        _updates.emit(name)
    }

    /**
     * Copy given data set and mirror its changes to this [ActiveDataTree] in [this@setAndObserve]. Returns an update [Job]
     */
    public fun CoroutineScope.setAndObserve(name: Name, dataSet: DataSet<T>): Job = launch {
        emit(name, dataSet)
        dataSet.updates.collect { nameInBranch ->
            emit(name + nameInBranch, dataSet.getData(nameInBranch))
        }
    }
}

/**
 * Create a dynamic tree. Initial data is placed synchronously. Updates are propagated via [updatesScope]
 */
@Suppress("FunctionName")
public suspend fun <T : Any> ActiveDataTree(
    type: KClass<out T>,
    block: suspend ActiveDataTree<T>.() -> Unit,
): DataTree<T> {
    val tree = ActiveDataTree(type)
    tree.block()
    return tree
}

@Suppress("FunctionName")
public suspend inline fun <reified T : Any> ActiveDataTree(
    crossinline block: suspend ActiveDataTree<T>.() -> Unit,
): DataTree<T> = ActiveDataTree(T::class).apply { block() }


public suspend inline fun <reified T : Any> ActiveDataTree<T>.emit(
    name: Name,
    noinline block: suspend ActiveDataTree<T>.() -> Unit,
): Unit = emit(name, ActiveDataTree(T::class, block))

public suspend inline fun <reified T : Any> ActiveDataTree<T>.emit(
    name: String,
    noinline block: suspend ActiveDataTree<T>.() -> Unit,
): Unit = emit(name.toName(), ActiveDataTree(T::class, block))
