package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.reflect.KType

public sealed class DataTreeItem<out T : Any> {
    public class Node<out T : Any>(public val tree: DataTree<T>) : DataTreeItem<T>()
    public class Leaf<out T : Any>(public val data: Data<T>) : DataTreeItem<T>()
}

public val <T : Any> DataTreeItem<T>.type: KType
    get() = when (this) {
        is DataTreeItem.Node -> tree.dataType
        is DataTreeItem.Leaf -> data.type
    }

/**
 * A tree-like [DataSet] grouped into the node. All data inside the node must inherit its type
 */
@Type(DataTree.TYPE)
public interface DataTree<out T : Any> : DataSet<T> {

    /**
     * Children items of this [DataTree] provided asynchronously
     */
    public suspend fun items(): Map<NameToken, DataTreeItem<T>>

    override fun flow(): Flow<NamedData<T>> = flow {
        items().forEach { (token, childItem: DataTreeItem<T>) ->
            if(!token.body.startsWith("@")) {
                when (childItem) {
                    is DataTreeItem.Leaf -> emit(childItem.data.named(token.asName()))
                    is DataTreeItem.Node -> emitAll(childItem.tree.flow().map { it.named(token + it.name) })
                }
            }
        }
    }

    override suspend fun listChildren(prefix: Name): List<Name> =
        getItem(prefix).tree?.items()?.keys?.map { prefix + it } ?: emptyList()

    override suspend fun getData(name: Name): Data<T>? = when (name.length) {
        0 -> null
        1 -> items()[name.firstOrNull()!!].data
        else -> items()[name.firstOrNull()!!].tree?.getData(name.cutFirst())
    }

    public companion object {
        public const val TYPE: String = "dataTree"
    }
}

public suspend fun <T: Any> DataSet<T>.getData(name: String): Data<T>? = getData(name.toName())

/**
 * Get a [DataTreeItem] with given [name] or null if the item does not exist
 */
public tailrec suspend fun <T : Any> DataTree<T>.getItem(name: Name): DataTreeItem<T>? = when (name.length) {
    0 -> DataTreeItem.Node(this)
    1 -> items()[name.firstOrNull()]
    else -> items()[name.firstOrNull()!!].tree?.getItem(name.cutFirst())
}

public val <T : Any> DataTreeItem<T>?.tree: DataTree<T>? get() = (this as? DataTreeItem.Node<T>)?.tree
public val <T : Any> DataTreeItem<T>?.data: Data<T>? get() = (this as? DataTreeItem.Leaf<T>)?.data

/**
 * Flow of all children including nodes
 */
public fun <T : Any> DataTree<T>.itemFlow(): Flow<Pair<Name, DataTreeItem<T>>> = flow {
    items().forEach { (head, item) ->
        emit(head.asName() to item)
        if (item is DataTreeItem.Node) {
            val subSequence = item.tree.itemFlow()
                .map { (name, data) -> (head.asName() + name) to data }
            emitAll(subSequence)
        }
    }
}

/**
 * Get a branch of this [DataTree] with a given [branchName].
 * The difference from similar method for [DataSet] is that internal logic is more simple and the return value is a [DataTree]
 */
public fun <T : Any> DataTree<T>.branch(branchName: Name): DataTree<T> = object : DataTree<T> {
    override val dataType: KType get() = this@branch.dataType

    override suspend fun items(): Map<NameToken, DataTreeItem<T>> = getItem(branchName).tree?.items() ?: emptyMap()
}
