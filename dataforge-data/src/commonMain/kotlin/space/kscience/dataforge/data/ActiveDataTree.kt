package space.kscience.dataforge.data

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A mutable [DataTree].
 */
public class ActiveDataTree<T : Any>(
    override val dataType: KType,
) : DataTree<T>, DataSetBuilder<T>, ActiveDataSet<T> {
    private val mutex = Mutex()
    private val treeItems = HashMap<NameToken, DataTreeItem<T>>()

    override val items: Map<NameToken, DataTreeItem<T>>
        get() = treeItems.filter { !it.key.body.startsWith("@") }

    private val _updates = MutableSharedFlow<Name>()

    override val updates: Flow<Name>
        get() = _updates

    private suspend fun remove(token: NameToken) = mutex.withLock {
        if (treeItems.remove(token) != null) {
            _updates.emit(token.asName())
        }
    }

    override suspend fun remove(name: Name) {
        if (name.isEmpty()) error("Can't remove the root node")
        (getItem(name.cutLast()).tree as? ActiveDataTree)?.remove(name.lastOrNull()!!)
    }

    private suspend fun set(token: NameToken, data: Data<T>) = mutex.withLock {
        treeItems[token] = DataTreeItem.Leaf(data)
    }

    private suspend fun getOrCreateNode(token: NameToken): ActiveDataTree<T> =
        (treeItems[token] as? DataTreeItem.Node<T>)?.tree as? ActiveDataTree<T>
            ?: ActiveDataTree<T>(dataType).also {
                mutex.withLock {
                    treeItems[token] = DataTreeItem.Node(it)
                }
            }

    private suspend fun getOrCreateNode(name: Name): ActiveDataTree<T> = when (name.length) {
        0 -> this
        1 -> getOrCreateNode(name.firstOrNull()!!)
        else -> getOrCreateNode(name.firstOrNull()!!).getOrCreateNode(name.cutFirst())
    }

    override suspend fun data(name: Name, data: Data<T>?) {
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

    override suspend fun meta(name: Name, meta: Meta) {
        val item = getItem(name)
        if(item is DataTreeItem.Leaf) error("TODO: Can't change meta of existing leaf item.")
        data(name + DataTree.META_ITEM_NAME_TOKEN, Data.empty(meta))
    }
}

/**
 * Create a dynamic tree. Initial data is placed synchronously. Updates are propagated via [updatesScope]
 */
@Suppress("FunctionName")
public suspend fun <T : Any> ActiveDataTree(
    type: KType,
    block: suspend ActiveDataTree<T>.() -> Unit,
): ActiveDataTree<T> {
    val tree = ActiveDataTree<T>(type)
    tree.block()
    return tree
}

@Suppress("FunctionName")
public suspend inline fun <reified T : Any> ActiveDataTree(
    crossinline block: suspend ActiveDataTree<T>.() -> Unit,
): ActiveDataTree<T> = ActiveDataTree<T>(typeOf<T>()).apply { block() }

public suspend inline fun <reified T : Any> ActiveDataTree<T>.emit(
    name: Name,
    noinline block: suspend ActiveDataTree<T>.() -> Unit,
): Unit = node(name, ActiveDataTree(typeOf<T>(), block))

public suspend inline fun <reified T : Any> ActiveDataTree<T>.emit(
    name: String,
    noinline block: suspend ActiveDataTree<T>.() -> Unit,
): Unit = node(Name.parse(name), ActiveDataTree(typeOf<T>(), block))
