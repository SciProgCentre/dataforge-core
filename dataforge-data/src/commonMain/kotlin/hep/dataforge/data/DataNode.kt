package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.*
import hep.dataforge.type.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
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
@Type(DataNode.TYPE)
public interface DataNode<out T : Any> : MetaRepr {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val type: KClass<out T>

    public val items: Map<NameToken, DataItem<T>>

    public val meta: Meta

    override fun toMeta(): Meta = Meta {
        "type" put (type.simpleName ?: "undefined")
        "meta" put meta
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
                is DataItem.Leaf<*> -> it.data.run { startAsync(this@launch) }
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
public fun <T : Any> DataNode<T>.itemSequence(): Sequence<Pair<Name, DataItem<T>>> = sequence {
    items.forEach { (head, item) ->
        yield(head.asName() to item)
        if (item is DataItem.Node) {
            val subSequence = item.node.itemSequence()
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

public fun <T : Any> DataNode<T>.filter(predicate: (Name, Data<T>) -> Boolean): DataNode<T> = DataNode.invoke(type) {
    dataSequence().forEach { (name, data) ->
        if (predicate(name, data)) {
            this[name] = data
        }
    }
}

public fun <T : Any> DataNode<T>.first(): Data<T>? = dataSequence().firstOrNull()?.second


public operator fun <T : Any> DataNode<T>.iterator(): Iterator<Pair<Name, DataItem<T>>> = itemSequence().iterator()

