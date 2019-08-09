package hep.dataforge.data

import hep.dataforge.names.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass

sealed class DataItem<out T : Any> {
    abstract val type: KClass<out T>

    class Node<out T : Any>(val value: DataNode<T>) : DataItem<T>() {
        override val type: KClass<out T> get() = value.type
    }

    class Leaf<out T : Any>(val value: Data<T>) : DataItem<T>() {
        override val type: KClass<out T> get() = value.type
    }
}

/**
 * A tree-like data structure grouped into the node. All data inside the node must inherit its type
 */
interface DataNode<out T : Any> {

    /**
     * The minimal common ancestor to all data in the node
     */
    val type: KClass<out T>

    val items: Map<NameToken, DataItem<T>>

    companion object {
        const val TYPE = "dataNode"

        fun <T : Any> build(type: KClass<out T>, block: DataTreeBuilder<T>.() -> Unit) =
            DataTreeBuilder(type).apply(block).build()

        fun <T : Any> builder(type: KClass<out T>) = DataTreeBuilder(type)
    }
}

val <T : Any> DataItem<T>?.node: DataNode<T>? get() = (this as? DataItem.Node<T>)?.value
val <T : Any> DataItem<T>?.data: Data<T>? get() = (this as? DataItem.Leaf<T>)?.value

/**
 * Start computation for all goals in data node
 */
fun DataNode<*>.startAll(scope: CoroutineScope): Unit = items.values.forEach {
    when (it) {
        is DataItem.Node<*> -> it.value.startAll(scope)
        is DataItem.Leaf<*> -> it.value.start(scope)
    }
}

fun DataNode<*>.joinAll(scope: CoroutineScope): Job = scope.launch {
    startAll(scope)
    items.forEach {
        when (val value = it.value) {
            is DataItem.Node -> value.value.joinAll(this).join()
            is DataItem.Leaf -> value.value.await(scope)
        }
    }
}

operator fun <T : Any> DataNode<T>.get(name: Name): DataItem<T>? = when (name.length) {
    0 -> error("Empty name")
    1 -> (items[name.first()] as? DataItem.Leaf)
    else -> get(name.first()!!.asName()).node?.get(name.cutFirst())
}

/**
 * Sequence of all children including nodes
 */
fun <T : Any> DataNode<T>.asSequence(): Sequence<Pair<Name, DataItem<T>>> = sequence {
    items.forEach { (head, item) ->
        yield(head.asName() to item)
        if (item is DataItem.Node) {
            val subSequence = item.value.asSequence()
                .map { (name, data) -> (head.asName() + name) to data }
            yieldAll(subSequence)
        }
    }
}

/**
 * Sequence of data entries
 */
fun <T : Any> DataNode<T>.dataSequence(): Sequence<Pair<Name, Data<T>>> = sequence {
    items.forEach { (head, item) ->
        when (item) {
            is DataItem.Leaf -> yield(head.asName() to item.value)
            is DataItem.Node -> {
                val subSequence = item.value.dataSequence()
                    .map { (name, data) -> (head.asName() + name) to data }
                yieldAll(subSequence)
            }
        }
    }
}

operator fun <T : Any> DataNode<T>.iterator(): Iterator<Pair<Name, DataItem<T>>> = asSequence().iterator()

class DataTree<out T : Any> internal constructor(
    override val type: KClass<out T>,
    override val items: Map<NameToken, DataItem<T>>
) : DataNode<T> {
    //TODO add node-level meta?
}

private sealed class DataTreeBuilderItem<out T : Any> {
    class Node<T : Any>(val tree: DataTreeBuilder<T>) : DataTreeBuilderItem<T>()
    class Leaf<T : Any>(val value: Data<T>) : DataTreeBuilderItem<T>()
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
        map[token] = DataTreeBuilderItem.Leaf(data)
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

    operator fun set(name: Name, item: DataItem<T>) = when (item) {
        is DataItem.Node<T> -> set(name, item.value.builder())
        is DataItem.Leaf<T> -> set(name, item.value)
    }

    /**
     * Append data to node
     */
    infix fun String.to(data: Data<T>) = set(toName(), data)

    /**
     * Append node
     */
    infix fun String.to(node: DataNode<T>) = set(toName(), node)

    infix fun String.to(item: DataItem<T>) = set(toName(), item)

    /**
     * Build and append node
     */
    infix fun String.to(block: DataTreeBuilder<T>.() -> Unit) = set(toName(), DataTreeBuilder<T>(type).apply(block))


    fun update(node: DataNode<T>) {
        node.dataSequence().forEach {
            //TODO check if the place is occupied
            this[it.first] = it.second
        }
    }

    fun build(): DataTree<T> {
        val resMap = map.mapValues { (_, value) ->
            when (value) {
                is DataTreeBuilderItem.Leaf -> DataItem.Leaf(value.value)
                is DataTreeBuilderItem.Node -> DataItem.Node(value.tree.build())
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

fun <T : Any> DataNode<T>.filter(predicate: (Name, Data<T>) -> Boolean): DataNode<T> = DataNode.build(type) {
    dataSequence().forEach { (name, data) ->
        if (predicate(name, data)) {
            this[name] = data
        }
    }
}

fun <T : Any> DataNode<T>.first(): Data<T>? = dataSequence().first().second

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
expect fun DataNode<*>.checkType(type: KClass<*>)