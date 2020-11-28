package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass

public sealed class DataItem<out T : Any> : MetaRepr {
    public abstract val type: KClass<out T>

    public abstract val meta: Meta

    public class Node<out T : Any>(public val node: DataNode<T>) : DataItem<T>() {
        override val type: KClass<out T> get() = node.type

        override fun toMeta(): Meta = node.toMeta()

        override val meta: Meta get() = node.meta
    }

    public class Leaf<out T : Any>(public val data: Data<T>) : DataItem<T>() {
        override val type: KClass<out T> get() = data.type

        override fun toMeta(): Meta = data.toMeta()

        override val meta: Meta get() = data.meta
    }
}

/**
 * A tree-like data structure grouped into the node. All data inside the node must inherit its type
 */
public interface DataNode<out T : Any> : MetaRepr {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val type: KClass<out T>

    public val items: Map<NameToken, DataItem<T>>

    public val meta: Meta

    override fun toMeta(): Meta = Meta {
        "type" put (type.simpleName ?: "undefined")
        "items" put {
            this@DataNode.items.forEach {
                it.key.toString() put it.value.toMeta()
            }
        }
    }

    /**
     * Start computation for all goals in data node and return a job for the whole node
     */
    @Suppress("DeferredResultUnused")
    public fun CoroutineScope.startAll(): Job = launch {
        items.values.forEach {
            when (it) {
                is DataItem.Node<*> -> it.node.run { startAll() }
                is DataItem.Leaf<*> -> it.data.run { startAsync() }
            }
        }
    }

    public companion object {
        public const val TYPE: String = "dataNode"

        public operator fun <T : Any> invoke(type: KClass<out T>, block: DataTreeBuilder<T>.() -> Unit): DataTree<T> =
            DataTreeBuilder(type).apply(block).build()

        public inline operator fun <reified T : Any> invoke(noinline block: DataTreeBuilder<T>.() -> Unit): DataTree<T> =
            DataTreeBuilder(T::class).apply(block).build()

        public fun <T : Any> builder(type: KClass<out T>): DataTreeBuilder<T> = DataTreeBuilder(type)
    }
}

public suspend fun <T: Any> DataNode<T>.join(): Unit = coroutineScope { startAll().join() }

public val <T : Any> DataItem<T>?.node: DataNode<T>? get() = (this as? DataItem.Node<T>)?.node
public val <T : Any> DataItem<T>?.data: Data<T>? get() = (this as? DataItem.Leaf<T>)?.data

public operator fun <T : Any> DataNode<T>.get(name: Name): DataItem<T>? = when (name.length) {
    0 -> error("Empty name")
    1 -> items[name.firstOrNull()]
    else -> get(name.firstOrNull()!!.asName()).node?.get(name.cutFirst())
}

public operator fun <T : Any> DataNode<T>.get(name: String): DataItem<T>? = get(name.toName())

/**
 * Sequence of all children including nodes
 */
public fun <T : Any> DataNode<T>.asSequence(): Sequence<Pair<Name, DataItem<T>>> = sequence {
    items.forEach { (head, item) ->
        yield(head.asName() to item)
        if (item is DataItem.Node) {
            val subSequence = item.node.asSequence()
                .map { (name, data) -> (head.asName() + name) to data }
            yieldAll(subSequence)
        }
    }
}

/**
 * Sequence of data entries
 */
public fun <T : Any> DataNode<T>.dataSequence(): Sequence<Pair<Name, Data<T>>> = sequence {
    items.forEach { (head, item) ->
        when (item) {
            is DataItem.Leaf -> yield(head.asName() to item.data)
            is DataItem.Node -> {
                val subSequence = item.node.dataSequence()
                    .map { (name, data) -> (head.asName() + name) to data }
                yieldAll(subSequence)
            }
        }
    }
}

public operator fun <T : Any> DataNode<T>.iterator(): Iterator<Pair<Name, DataItem<T>>> = asSequence().iterator()

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
    this[name] = DataNode(T::class, block)
}

public inline fun <reified T : Any> DataTreeBuilder<T>.node(name: String, noinline block: DataTreeBuilder<T>.() -> Unit) {
    this[name.toName()] = DataNode(T::class, block)
}

/**
 * Generate a mutable builder from this node. Node content is not changed
 */
public fun <T : Any> DataNode<T>.builder(): DataTreeBuilder<T> = DataTreeBuilder(type).apply {
    dataSequence().forEach { (name, data) -> this[name] = data }
}

public fun <T : Any> DataNode<T>.filter(predicate: (Name, Data<T>) -> Boolean): DataNode<T> = DataNode.invoke(type) {
    dataSequence().forEach { (name, data) ->
        if (predicate(name, data)) {
            this[name] = data
        }
    }
}

public fun <T : Any> DataNode<T>.first(): Data<T>? = dataSequence().first().second