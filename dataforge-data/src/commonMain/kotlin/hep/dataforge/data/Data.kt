package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import kotlin.coroutines.CoroutineContext

/**
 * A data element characterized by its meta
 */
interface Data<out T> : MetaRepr {
    val meta: Meta
    val goal: Goal<T>

    override fun toMeta(): Meta = meta

    companion object {
        fun <T> of(meta: Meta, goal: Goal<T>): Data<T> = DataImpl(meta, goal)
        fun <T> of(name: String, meta: Meta, goal: Goal<T>): Data<T> = NamedData(name, of(meta, goal))
        fun <T> static(context: CoroutineContext, meta: Meta, value: T): Data<T> = DataImpl(meta, Goal.static(context, value))
    }
}

/**
 * Generic Data implementation
 */
private class DataImpl<out T>(override val meta: Meta, override val goal: Goal<T>) : Data<T>

class NamedData<out T>(val name: String, data: Data<T>) : Data<T> by data

/**
 * A tree-like data structure grouped into the node. All data inside the node must inherit its type
 */
interface DataNode<out T> {
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
    fun asSequence(): Sequence<Pair<Name, Data<T>>>

    operator fun iterator(): Iterator<Pair<Name, Data<T>>> = asSequence().iterator()

    companion object {
        fun <T> build(block: DataTreeBuilder<T>.() -> Unit) = DataTreeBuilder<T>().apply(block).build()
    }

}

internal sealed class DataTreeItem<out T> {
    class Node<out T>(val tree: DataTree<T>) : DataTreeItem<T>()
    class Value<out T>(val value: Data<T>) : DataTreeItem<T>()
}

class DataTree<out T> internal constructor(private val items: Map<NameToken, DataTreeItem<T>>) : DataNode<T> {
    //TODO add node-level meta?

    override fun get(name: Name): Data<T>? = when (name.length) {
        0 -> error("Empty name")
        1 -> (items[name.first()] as? DataTreeItem.Value)?.value
        else -> getNode(name.first()!!.toName())?.get(name.cutFirst())
    }

    override fun getNode(name: Name): DataTree<T>? = when (name.length) {
        0 -> this
        1 -> (items[name.first()] as? DataTreeItem.Node)?.tree
        else -> getNode(name.first()!!.toName())?.getNode(name.cutFirst())
    }

    override fun asSequence(): Sequence<Pair<Name, Data<T>>> {
        return kotlin.sequences.sequence {
            items.forEach { (head, tree) ->
                when (tree) {
                    is DataTreeItem.Value -> yield(head.toName() to tree.value)
                    is DataTreeItem.Node -> {
                        val subSequence = tree.tree.asSequence().map { (name, data) -> (head.toName() + name) to data }
                        yieldAll(subSequence)
                    }
                }
            }
        }
    }
}

private sealed class DataTreeBuilderItem<out T> {
    class Node<T>(val tree: DataTreeBuilder<T>) : DataTreeBuilderItem<T>()
    class Value<T>(val value: Data<T>) : DataTreeBuilderItem<T>()
}

/**
 * A builder for a DataTree.
 */
class DataTreeBuilder<T> {
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
            DataTreeBuilder<T>().also { map.put(token, DataTreeBuilderItem.Node(it)) }
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
    infix fun String.to(block: DataTreeBuilder<T>.() -> Unit) = set(toName(), DataTreeBuilder<T>().apply(block))

    fun build(): DataTree<T> {
        val resMap = map.mapValues { (_, value) ->
            when (value) {
                is DataTreeBuilderItem.Value -> DataTreeItem.Value(value.value)
                is DataTreeBuilderItem.Node -> DataTreeItem.Node(value.tree.build())
            }
        }
        return DataTree(resMap)
    }
}

/**
 * Generate a mutable builder from this node. Node content is not changed
 */
fun <T> DataNode<T>.builder(): DataTreeBuilder<T> = DataTreeBuilder<T>().apply {
    asSequence().forEach { (name, data) -> this[name] = data }
}

/**
 * Start computation for all goals in data node
 */
fun DataNode<*>.startAll() = asSequence().forEach { (_,data)->data.goal.start() }