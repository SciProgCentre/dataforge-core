package hep.dataforge.data

import hep.dataforge.names.*
import kotlin.reflect.KClass

/**
 * A tree-like data structure grouped into the node. All data inside the node must inherit its type
 */
interface DataNode<out T : Any> {

    /**
     * The minimal common ancestor to all data in the node
     */
    val type: KClass<out T>

    /**
     * Get the specific data if it exists
     */
    operator fun get(name: Name): Data<T>?

    /**
     * Get a subnode with given name if it exists.
     */
    fun getNode(name: Name): DataNode<T>?

    /**
     * Walk the tree upside down and provide all data nodes with full names
     */
    fun dataSequence(): Sequence<Pair<Name, Data<T>>>

    /**
     * A sequence of all nodes in the tree walking upside down, excluding self
     */
    fun nodeSequence(): Sequence<Pair<Name, DataNode<T>>>

    operator fun iterator(): Iterator<Pair<Name, Data<T>>> = dataSequence().iterator()

    companion object {
        const val TYPE = "dataNode"

        fun <T : Any> build(type: KClass<out T>, block: DataTreeBuilder<T>.() -> Unit) =
            DataTreeBuilder<T>(type).apply(block).build()

        fun <T : Any> builder(type: KClass<out T>) = DataTreeBuilder(type)
    }

}

internal sealed class DataTreeItem<out T : Any> {
    class Node<out T : Any>(val tree: DataTree<T>) : DataTreeItem<T>()
    class Value<out T : Any>(val value: Data<T>) : DataTreeItem<T>()
}

class DataTree<out T : Any> internal constructor(
    override val type: KClass<out T>,
    private val items: Map<NameToken, DataTreeItem<T>>
) : DataNode<T> {
    //TODO add node-level meta?

    override fun get(name: Name): Data<T>? = when (name.length) {
        0 -> error("Empty name")
        1 -> (items[name.first()] as? DataTreeItem.Value)?.value
        else -> getNode(name.first()!!.asName())?.get(name.cutFirst())
    }

    override fun getNode(name: Name): DataTree<T>? = when (name.length) {
        0 -> this
        1 -> (items[name.first()] as? DataTreeItem.Node)?.tree
        else -> getNode(name.first()!!.asName())?.getNode(name.cutFirst())
    }

    override fun dataSequence(): Sequence<Pair<Name, Data<T>>> {
        return sequence {
            items.forEach { (head, tree) ->
                when (tree) {
                    is DataTreeItem.Value -> yield(head.asName() to tree.value)
                    is DataTreeItem.Node -> {
                        val subSequence =
                            tree.tree.dataSequence().map { (name, data) -> (head.asName() + name) to data }
                        yieldAll(subSequence)
                    }
                }
            }
        }
    }

    override fun nodeSequence(): Sequence<Pair<Name, DataNode<T>>> {
        return sequence {
            items.forEach { (head, tree) ->
                if (tree is DataTreeItem.Node) {
                    yield(head.asName() to tree.tree)
                    val subSequence =
                        tree.tree.nodeSequence().map { (name, node) -> (head.asName() + name) to node }
                    yieldAll(subSequence)
                }
            }
        }
    }
}

private sealed class DataTreeBuilderItem<out T : Any> {
    class Node<T : Any>(val tree: DataTreeBuilder<T>) : DataTreeBuilderItem<T>()
    class Value<T : Any>(val value: Data<T>) : DataTreeBuilderItem<T>()
}

/**
 * A builder for a DataTree.
 */
class DataTreeBuilder<T : Any>(private val type: KClass<out T>) {
    private val map = HashMap<NameToken, DataTreeBuilderItem<T>>()

    operator fun set(token: NameToken, node: DataTreeBuilder<T>) {
        if (map.containsKey(token)) error("Tree entry with name $token is not empty")
        map[token] = DataTreeBuilderItem.Node(node)
    }

    operator fun set(token: NameToken, data: Data<T>) {
        if (map.containsKey(token)) error("Tree entry with name $token is not empty")
        map[token] = DataTreeBuilderItem.Value(data)
    }

    private fun buildNode(token: NameToken): DataTreeBuilder<T> {
        return if (!map.containsKey(token)) {
            DataTreeBuilder<T>(type).also { map[token] = DataTreeBuilderItem.Node(it) }
        } else {
            (map[token] as? DataTreeBuilderItem.Node ?: error("The node with name $token is occupied by leaf")).tree
        }
    }

    private fun buildNode(name: Name): DataTreeBuilder<T> {
        return when (name.length) {
            0 -> this
            1 -> buildNode(name.first()!!)
            else -> buildNode(name.first()!!).buildNode(name.cutFirst())
        }
    }

    operator fun set(name: Name, data: Data<T>) {
        when (name.length) {
            0 -> error("Can't add data with empty name")
            1 -> set(name.first()!!, data)
            2 -> buildNode(name.cutLast())[name.last()!!] = data
        }
    }

    operator fun set(name: Name, node: DataTreeBuilder<T>) {
        when (name.length) {
            0 -> error("Can't add data with empty name")
            1 -> set(name.first()!!, node)
            2 -> buildNode(name.cutLast())[name.last()!!] = node
        }
    }

    operator fun set(name: Name, node: DataNode<T>) = set(name, node.builder())

    /**
     * Append data to node
     */
    infix fun String.to(data: Data<T>) = set(toName(), data)

    /**
     * Append node
     */
    infix fun String.to(node: DataNode<T>) = set(toName(), node)

    /**
     * Build and append node
     */
    infix fun String.to(block: DataTreeBuilder<T>.() -> Unit) = set(toName(), DataTreeBuilder<T>(type).apply(block))

    fun build(): DataTree<T> {
        val resMap = map.mapValues { (_, value) ->
            when (value) {
                is DataTreeBuilderItem.Value -> DataTreeItem.Value(value.value)
                is DataTreeBuilderItem.Node -> DataTreeItem.Node(value.tree.build())
            }
        }
        return DataTree(type, resMap)
    }
}

/**
 * Generate a mutable builder from this node. Node content is not changed
 */
fun <T : Any> DataNode<T>.builder(): DataTreeBuilder<T> = DataTreeBuilder(type).apply {
    dataSequence().forEach { (name, data) -> this[name] = data }
}

/**
 * Start computation for all goals in data node
 */
fun DataNode<*>.startAll() = dataSequence().forEach { (_, data) -> data.goal.start() }

fun <T : Any> DataNode<T>.filter(predicate: (Name, Data<T>) -> Boolean): DataNode<T> = DataNode.build(type) {
    dataSequence().forEach { (name, data) ->
        if (predicate(name, data)) {
            this[name] = data
        }
    }
}

//fun <T : Any, R: T> DataNode<T>.filterIsInstance(type: KClass<R>): DataNode<R> = filter{_,data -> type.}