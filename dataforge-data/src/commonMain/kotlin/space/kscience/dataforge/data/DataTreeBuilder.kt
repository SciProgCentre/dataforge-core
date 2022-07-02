package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.*
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.jvm.Synchronized
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public interface DataSourceBuilder<T : Any> : DataSetBuilder<T>, DataSource<T> {
    override val updates: MutableSharedFlow<Name>
}

/**
 * A mutable [DataTree] that propagates updates
 */
public class DataTreeBuilder<T : Any>(
    override val dataType: KType,
    coroutineContext: CoroutineContext,
) : DataTree<T>, DataSourceBuilder<T> {

    override val coroutineContext: CoroutineContext =
        coroutineContext + Job(coroutineContext[Job]) + GoalExecutionRestriction()

    private val treeItems = HashMap<NameToken, DataTreeItem<T>>()

    override val items: Map<NameToken, DataTreeItem<T>>
        get() = treeItems.filter { !it.key.body.startsWith("@") }

    override val updates: MutableSharedFlow<Name> = MutableSharedFlow<Name>()

    @Synchronized
    private fun remove(token: NameToken) {
        if (treeItems.remove(token) != null) {
            launch {
                updates.emit(token.asName())
            }
        }
    }

    override fun remove(name: Name) {
        if (name.isEmpty()) error("Can't remove the root node")
        (getItem(name.cutLast()).tree as? DataTreeBuilder)?.remove(name.lastOrNull()!!)
    }

    @Synchronized
    private fun set(token: NameToken, data: Data<T>) {
        treeItems[token] = DataTreeItem.Leaf(data)
    }

    @Synchronized
    private fun set(token: NameToken, node: DataTree<T>) {
        treeItems[token] = DataTreeItem.Node(node)
    }

    private fun getOrCreateNode(token: NameToken): DataTreeBuilder<T> =
        (treeItems[token] as? DataTreeItem.Node<T>)?.tree as? DataTreeBuilder<T>
            ?: DataTreeBuilder<T>(dataType, coroutineContext).also { set(token, it) }

    private fun getOrCreateNode(name: Name): DataTreeBuilder<T> = when (name.length) {
        0 -> this
        1 -> getOrCreateNode(name.firstOrNull()!!)
        else -> getOrCreateNode(name.firstOrNull()!!).getOrCreateNode(name.cutFirst())
    }

    override fun data(name: Name, data: Data<T>?) {
        if (data == null) {
            remove(name)
        } else {
            when (name.length) {
                0 -> error("Can't add data with empty name")
                1 -> set(name.firstOrNull()!!, data)
                2 -> getOrCreateNode(name.cutLast()).set(name.lastOrNull()!!, data)
            }
        }
        launch {
            updates.emit(name)
        }
    }

    override fun meta(name: Name, meta: Meta) {
        val item = getItem(name)
        if (item is DataTreeItem.Leaf) error("TODO: Can't change meta of existing leaf item.")
        data(name + DataTree.META_ITEM_NAME_TOKEN, Data.empty(meta))
    }
}

/**
 * Create a dynamic [DataSource]. Initial data is placed synchronously.
 */
@DFInternal
@Suppress("FunctionName")
public fun <T : Any> DataSource(
    type: KType,
    parent: CoroutineScope,
    block: DataSourceBuilder<T>.() -> Unit,
): DataTreeBuilder<T> = DataTreeBuilder<T>(type, parent.coroutineContext).apply(block)

@Suppress("OPT_IN_USAGE","FunctionName")
public inline fun <reified T : Any> DataSource(
    parent: CoroutineScope,
    crossinline block: DataSourceBuilder<T>.() -> Unit,
): DataTreeBuilder<T> = DataSource(typeOf<T>(), parent) { block() }

@Suppress("FunctionName")
public suspend inline fun <reified T : Any> DataSource(
    crossinline block: DataSourceBuilder<T>.() -> Unit = {},
): DataTreeBuilder<T> = DataTreeBuilder<T>(typeOf<T>(), coroutineContext).apply { block() }

public inline fun <reified T : Any> DataSourceBuilder<T>.emit(
    name: Name,
    parent: CoroutineScope,
    noinline block: DataSourceBuilder<T>.() -> Unit,
): Unit = node(name, DataSource(parent, block))

public inline fun <reified T : Any> DataSourceBuilder<T>.emit(
    name: String,
    parent: CoroutineScope,
    noinline block: DataSourceBuilder<T>.() -> Unit,
): Unit = node(Name.parse(name), DataSource(parent, block))
