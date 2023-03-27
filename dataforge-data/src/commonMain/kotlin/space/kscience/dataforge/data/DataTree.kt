package space.kscience.dataforge.data

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public sealed class DataTreeItem<out T : Any> {

    public abstract val meta: Meta

    public class Node<out T : Any>(public val tree: DataTree<T>) : DataTreeItem<T>() {
        override val meta: Meta get() = tree.meta
    }

    public class Leaf<out T : Any>(public val data: Data<T>) : DataTreeItem<T>() {
        override val meta: Meta get() = data.meta
    }
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
     * Top-level children items of this [DataTree]
     */
    public val items: Map<NameToken, DataTreeItem<T>>

    override val meta: Meta get() = items[META_ITEM_NAME_TOKEN]?.meta ?: Meta.EMPTY

    override fun iterator(): Iterator<NamedData<T>> = iterator {
        items.forEach { (token, childItem: DataTreeItem<T>) ->
            if (!token.body.startsWith("@")) {
                when (childItem) {
                    is DataTreeItem.Leaf -> yield(childItem.data.named(token.asName()))
                    is DataTreeItem.Node -> yieldAll(childItem.tree.asSequence().map { it.named(token + it.name) })
                }
            }
        }
    }

    override fun get(name: Name): Data<T>? = when (name.length) {
        0 -> null
        1 -> items[name.firstOrNull()!!].data
        else -> items[name.firstOrNull()!!].tree?.get(name.cutFirst())
    }

    public companion object {
        public const val TYPE: String = "dataTree"

        /**
         * A name token used to designate tree node meta
         */
        public val META_ITEM_NAME_TOKEN: NameToken = NameToken("@meta")

        @DFInternal
        public fun <T : Any> emptyWithType(type: KType, meta: Meta = Meta.EMPTY): DataTree<T> = object : DataTree<T> {
            override val items: Map<NameToken, DataTreeItem<T>> get() = emptyMap()
            override val dataType: KType get() = type
            override val meta: Meta get() = meta
        }

        @OptIn(DFInternal::class)
        public inline fun <reified T : Any> empty(meta: Meta = Meta.EMPTY): DataTree<T> =
            emptyWithType<T>(typeOf<T>(), meta)
    }
}

public fun <T : Any> DataTree<T>.listChildren(prefix: Name): List<Name> =
    getItem(prefix).tree?.items?.keys?.map { prefix + it } ?: emptyList()

/**
 * Get a [DataTreeItem] with given [name] or null if the item does not exist
 */
public tailrec fun <T : Any> DataTree<T>.getItem(name: Name): DataTreeItem<T>? = when (name.length) {
    0 -> DataTreeItem.Node(this)
    1 -> items[name.firstOrNull()]
    else -> items[name.firstOrNull()!!].tree?.getItem(name.cutFirst())
}

public val <T : Any> DataTreeItem<T>?.tree: DataTree<T>? get() = (this as? DataTreeItem.Node<T>)?.tree
public val <T : Any> DataTreeItem<T>?.data: Data<T>? get() = (this as? DataTreeItem.Leaf<T>)?.data

/**
 * A [Sequence] of all children including nodes
 */
public fun <T : Any> DataTree<T>.traverseItems(): Sequence<Pair<Name, DataTreeItem<T>>> = sequence {
    items.forEach { (head, item) ->
        yield(head.asName() to item)
        if (item is DataTreeItem.Node) {
            val subSequence = item.tree.traverseItems()
                .map { (name, data) -> (head.asName() + name) to data }
            yieldAll(subSequence)
        }
    }
}

/**
 * Get a branch of this [DataTree] with a given [branchName].
 * The difference from similar method for [DataSet] is that internal logic is more simple and the return value is a [DataTree]
 */
@OptIn(DFInternal::class)
public fun <T : Any> DataTree<T>.branch(branchName: Name): DataTree<T> =
    getItem(branchName)?.tree ?: DataTree.emptyWithType(dataType)

public fun <T : Any> DataTree<T>.branch(branchName: String): DataTree<T> = branch(branchName.parseAsName())
