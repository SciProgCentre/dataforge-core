package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.*
import kotlin.reflect.KClass

public class DataTree<out T : Any> internal constructor(
    override val type: KClass<out T>,
    override val items: Map<NameToken, DataItem<T>>,
    override val meta: Meta
) : DataNode<T>

private sealed class DataTreeBuilderItem<out T : Any> {
    class Node<T : Any>(val tree: DataTreeBuilder<T>) : DataTreeBuilderItem<T>()
    class Leaf<T : Any>(val value: Data<T>) : DataTreeBuilderItem<T>()
}

/**
 * A builder for a DataTree.
 */
@DFBuilder
public class DataTreeBuilder<T : Any>(public val type: KClass<out T>) {
    private val map = HashMap<NameToken, DataTreeBuilderItem<T>>()

    private var meta = MetaBuilder()

    public operator fun set(token: NameToken, node: DataTreeBuilder<out T>) {
        if (map.containsKey(token)) error("Tree entry with name $token is not empty")
        map[token] = DataTreeBuilderItem.Node(node)
    }

    public operator fun set(token: NameToken, data: Data<T>) {
        if (map.containsKey(token)) error("Tree entry with name $token is not empty")
        map[token] = DataTreeBuilderItem.Leaf(data)
    }

    private fun buildNode(token: NameToken): DataTreeBuilder<T> {
        return if (!map.containsKey(token)) {
            DataTreeBuilder(type).also { map[token] = DataTreeBuilderItem.Node(it) }
        } else {
            (map[token] as? DataTreeBuilderItem.Node<T> ?: error("The node with name $token is occupied by leaf")).tree
        }
    }

    private fun buildNode(name: Name): DataTreeBuilder<T> {
        return when (name.length) {
            0 -> this
            1 -> buildNode(name.firstOrNull()!!)
            else -> buildNode(name.firstOrNull()!!).buildNode(name.cutFirst())
        }
    }

    public operator fun set(name: Name, data: Data<T>) {
        when (name.length) {
            0 -> error("Can't add data with empty name")
            1 -> set(name.firstOrNull()!!, data)
            2 -> buildNode(name.cutLast())[name.lastOrNull()!!] = data
        }
    }

    public operator fun set(name: Name, node: DataTreeBuilder<out T>) {
        when (name.length) {
            0 -> error("Can't add data with empty name")
            1 -> set(name.firstOrNull()!!, node)
            2 -> buildNode(name.cutLast())[name.lastOrNull()!!] = node
        }
    }

    public operator fun set(name: Name, node: DataNode<T>): Unit = set(name, node.builder())

    public operator fun set(name: Name, item: DataItem<T>): Unit = when (item) {
        is DataItem.Node<T> -> set(name, item.node.builder())
        is DataItem.Leaf<T> -> set(name, item.data)
    }

    /**
     * Append data to node
     */
    public infix fun String.put(data: Data<T>): Unit = set(toName(), data)

    /**
     * Append node
     */
    public infix fun String.put(node: DataNode<T>): Unit = set(toName(), node)

    public infix fun String.put(item: DataItem<T>): Unit = set(toName(), item)

    /**
     * Build and append node
     */
    public infix fun String.put(block: DataTreeBuilder<T>.() -> Unit): Unit = set(toName(), DataTreeBuilder(type).apply(block))


    /**
     * Update data with given node data and meta with node meta.
     */
    public fun update(node: DataNode<T>) {
        node.dataSequence().forEach {
            //TODO check if the place is occupied
            this[it.first] = it.second
        }
        meta.update(node.meta)
    }

    public fun meta(block: MetaBuilder.() -> Unit): MetaBuilder = meta.apply(block)

    public fun meta(meta: Meta) {
        this.meta = meta.builder()
    }

    public fun build(): DataTree<T> {
        val resMap = map.mapValues { (_, value) ->
            when (value) {
                is DataTreeBuilderItem.Leaf -> DataItem.Leaf(value.value)
                is DataTreeBuilderItem.Node -> DataItem.Node(value.tree.build())
            }
        }
        return DataTree(type, resMap, meta.seal())
    }
}

@Suppress("FunctionName")
public fun <T : Any> DataTree(type: KClass<out T>, block: DataTreeBuilder<T>.() -> Unit): DataTree<T> =
    DataTreeBuilder(type).apply(block).build()

@Suppress("FunctionName")
public inline fun <reified T : Any> DataTree(noinline block: DataTreeBuilder<T>.() -> Unit): DataTree<T> =
    DataTreeBuilder(T::class).apply(block).build()


public fun <T : Any> DataTreeBuilder<T>.datum(name: Name, data: Data<T>) {
    this[name] = data
}

public fun <T : Any> DataTreeBuilder<T>.datum(name: String, data: Data<T>) {
    this[name.toName()] = data
}

public fun <T : Any> DataTreeBuilder<T>.static(name: Name, data: T, meta: Meta = Meta.EMPTY) {
    this[name] = Data.static(data, meta)
}

public fun <T : Any> DataTreeBuilder<T>.static(name: Name, data: T, block: MetaBuilder.() -> Unit = {}) {
    this[name] = Data.static(data, Meta(block))
}

public fun <T : Any> DataTreeBuilder<T>.static(name: String, data: T, block: MetaBuilder.() -> Unit = {}) {
    this[name.toName()] = Data.static(data, Meta(block))
}

public fun <T : Any> DataTreeBuilder<T>.node(name: Name, node: DataNode<T>) {
    this[name] = node
}

public fun <T : Any> DataTreeBuilder<T>.node(name: String, node: DataNode<T>) {
    this[name.toName()] = node
}

public inline fun <reified T : Any> DataTreeBuilder<T>.node(name: Name, noinline block: DataTreeBuilder<T>.() -> Unit) {
    this[name] = DataTree(T::class, block)
}

public inline fun <reified T : Any> DataTreeBuilder<T>.node(name: String, noinline block: DataTreeBuilder<T>.() -> Unit) {
    this[name.toName()] = DataTree(T::class, block)
}

/**
 * Generate a mutable builder from this node. Node content is not changed
 */
public fun <T : Any> DataNode<T>.builder(): DataTreeBuilder<T> = DataTreeBuilder(type).apply {
    dataSequence().forEach { (name, data) -> this[name] = data }
}
