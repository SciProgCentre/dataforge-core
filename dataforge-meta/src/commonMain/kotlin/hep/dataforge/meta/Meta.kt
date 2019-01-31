package hep.dataforge.meta

import hep.dataforge.meta.Meta.Companion.VALUE_KEY
import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.meta.MetaItem.ValueItem
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import hep.dataforge.values.Value
import hep.dataforge.values.boolean


/**
 * A member of the meta tree. Could be represented as one of following:
 * * a [ValueItem] (leaf)
 * * a [NodeItem] (node)
 */
sealed class MetaItem<M : Meta> {
    data class ValueItem<M : Meta>(val value: Value) : MetaItem<M>()
    data class NodeItem<M : Meta>(val node: M) : MetaItem<M>()
}

/**
 * The object that could be represented as [Meta]. Meta provided by [toMeta] method should fully represent object state.
 * Meaning that two states with the same meta are equal.
 */
interface MetaRepr {
    fun toMeta(): Meta
}

/**
 * Generic meta tree representation. Elements are [MetaItem] objects that could be represented by three different entities:
 *  * [MetaItem.ValueItem] (leaf)
 *  * [MetaItem.NodeItem] single node
 *
 *   * Same name siblings are supported via elements with the same [Name] but different queries
 */
interface Meta : MetaRepr {
    val items: Map<NameToken, MetaItem<out Meta>>

    override fun toMeta(): Meta = this

    companion object {
        const val TYPE = "meta"
        /**
         * A key for single value node
         */
        const val VALUE_KEY = "@value"
    }
}

/* Get operations*/

/**
 * Fast [String]-based accessor for item map
 */
operator fun <T> Map<NameToken, T>.get(body: String, query: String = ""): T? = get(NameToken(body, query))

operator fun Meta.get(name: Name): MetaItem<out Meta>? {
    return name.first()?.let { token ->
        val tail = name.cutFirst()
        when (tail.length) {
            0 -> items[token]
            else -> items[token]?.node?.get(tail)
        }
    }
}

operator fun Meta.get(token: NameToken): MetaItem<out Meta>? = items[token]
operator fun Meta.get(key: String): MetaItem<out Meta>? = get(key.toName())

/**
 * Get all items matching given name.
 */
fun Meta.getAll(name: Name): Map<String, MetaItem<out Meta>> {
    if (name.length == 0) error("Can't use empty name for that")
    val (body, query) = name.last()!!
    val regex = query.toRegex()
    return (this[name.cutLast()] as? NodeItem<*>)?.node?.items
        ?.filter { it.key.body == body && (query.isEmpty() || regex.matches(it.key.query)) }
        ?.mapKeys { it.key.query }
        ?: emptyMap()

}

/**
 * Transform meta to sequence of [Name]-[Value] pairs
 */
fun Meta.asValueSequence(): Sequence<Pair<Name, Value>> {
    return items.asSequence().flatMap { entry ->
        val item = entry.value
        when (item) {
            is ValueItem -> sequenceOf(entry.key.toName() to item.value)
            is NodeItem -> item.node.asValueSequence().map { pair -> (entry.key.toName() + pair.first) to pair.second }
        }
    }
}

operator fun Meta.iterator(): Iterator<Pair<Name, Value>> = asValueSequence().iterator()

/**
 * A meta node that ensures that all of its descendants has at least the same type
 */
abstract class MetaNode<M : MetaNode<M>> : Meta {
    abstract override val items: Map<NameToken, MetaItem<M>>

    operator fun get(name: Name): MetaItem<M>? {
        return name.first()?.let { token ->
            val tail = name.cutFirst()
            when (tail.length) {
                0 -> items[token]
                else -> items[token]?.node?.get(tail)
            }
        }
    }

    operator fun get(key: String): MetaItem<M>? = get(key.toName())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Meta) return false

        return this.items == other.items
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }
}

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
class SealedMeta internal constructor(override val items: Map<NameToken, MetaItem<SealedMeta>>) : MetaNode<SealedMeta>()

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(items.mapValues { entry -> entry.value.seal() })

fun MetaItem<*>.seal(): MetaItem<SealedMeta> = when (this) {
    is MetaItem.ValueItem -> MetaItem.ValueItem(value)
    is MetaItem.NodeItem -> MetaItem.NodeItem(node.seal())
}

object EmptyMeta : Meta {
    override val items: Map<NameToken, MetaItem<out Meta>> = emptyMap()
}

/**
 * Unsafe methods to access values and nodes directly from [MetaItem]
 */

val MetaItem<*>.value
    get() = (this as? MetaItem.ValueItem)?.value
        ?: (this.node[VALUE_KEY] as? MetaItem.ValueItem)?.value
        ?: error("Trying to interpret node meta item as value item")
val MetaItem<*>.string get() = value.string
val MetaItem<*>.boolean get() = value.boolean
val MetaItem<*>.number get() = value.number
val MetaItem<*>.double get() = number.toDouble()
val MetaItem<*>.int get() = number.toInt()
val MetaItem<*>.long get() = number.toLong()
val MetaItem<*>.short get() = number.toShort()

val <M : Meta> MetaItem<M>.node: M
    get() = when (this) {
        is MetaItem.ValueItem -> error("Trying to interpret value meta item as node item")
        is MetaItem.NodeItem -> node
    }

/**
 * Generic meta-holder object
 */
interface Metoid {
    val meta: Meta
}

fun Value.toMeta() = buildMeta { Meta.VALUE_KEY to this }

fun Meta.isEmpty() = this === EmptyMeta || this.items.isEmpty()