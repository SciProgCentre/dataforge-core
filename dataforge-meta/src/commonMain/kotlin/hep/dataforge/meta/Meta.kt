package hep.dataforge.meta

import hep.dataforge.meta.Meta.Companion.VALUE_KEY
import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.meta.MetaItem.ValueItem
import hep.dataforge.names.*
import hep.dataforge.values.EnumValue
import hep.dataforge.values.Value
import hep.dataforge.values.boolean


/**
 * A member of the meta tree. Could be represented as one of following:
 * * a [ValueItem] (leaf)
 * * a [NodeItem] (node)
 */
sealed class MetaItem<out M : Meta> {
    data class ValueItem(val value: Value) : MetaItem<Nothing>() {
        override fun toString(): String = value.toString()
    }

    data class NodeItem<M : Meta>(val node: M) : MetaItem<M>() {
        override fun toString(): String = node.toString()
    }
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
    /**
     * Top level items of meta tree
     */
    val items: Map<NameToken, MetaItem<*>>

    override fun toMeta(): Meta = this

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String

    companion object {
        const val TYPE = "meta"
        /**
         * A key for single value node
         */
        const val VALUE_KEY = "@value"

        val empty: EmptyMeta = EmptyMeta
    }
}

/* Get operations*/

operator fun Meta?.get(name: Name): MetaItem<*>? {
    if (this == null) return null
    return name.first()?.let { token ->
        val tail = name.cutFirst()
        when (tail.length) {
            0 -> items[token]
            else -> items[token]?.node?.get(tail)
        }
    }
}

operator fun Meta?.get(token: NameToken): MetaItem<*>? = this?.items?.get(token)
operator fun Meta?.get(key: String): MetaItem<*>? = get(key.toName())

/**
 * Get a sequence of [Name]-[Value] pairs
 */
fun Meta.values(): Sequence<Pair<Name, Value>> {
    return items.asSequence().flatMap { (key, item) ->
        when (item) {
            is ValueItem -> sequenceOf(key.asName() to item.value)
            is NodeItem -> item.node.values().map { pair -> (key.asName() + pair.first) to pair.second }
        }
    }
}

/**
 * Get a sequence of all [Name]-[MetaItem] pairs for all items including nodes
 */
fun Meta.sequence(): Sequence<Pair<Name, MetaItem<*>>> {
    return sequence {
        items.forEach { (key, item) ->
            yield(key.asName() to item)
            if (item is NodeItem<*>) {
                yieldAll(item.node.sequence().map { (innerKey, innerItem) ->
                    (key + innerKey) to innerItem
                })
            }
        }
    }
}

operator fun Meta.iterator(): Iterator<Pair<Name, MetaItem<*>>> = sequence().iterator()

/**
 * A meta node that ensures that all of its descendants has at least the same type
 */
interface MetaNode<M : MetaNode<M>> : Meta {
    override val items: Map<NameToken, MetaItem<M>>
}

operator fun <M : MetaNode<M>> MetaNode<M>?.get(name: Name): MetaItem<M>? {
    if (this == null) return null
    return name.first()?.let { token ->
        val tail = name.cutFirst()
        when (tail.length) {
            0 -> items[token]
            else -> items[token]?.node?.get(tail)
        }
    }
}

operator fun <M : MetaNode<M>> MetaNode<M>?.get(key: String): MetaItem<M>? = if (this == null) {
    null
} else {
    this[key.toName()]
}

operator fun <M : MetaNode<M>> MetaNode<M>?.get(key: NameToken): MetaItem<M>? = if (this == null) {
    null
} else {
    this[key.asName()]
}

/**
 * Equals, hashcode and to string for any meta
 */
abstract class MetaBase : Meta {

    override fun equals(other: Any?): Boolean = if (other is Meta) {
        this.items == other.items
    } else {
        false
    }

    override fun hashCode(): Int = items.hashCode()

    override fun toString(): String = items.toString()
}

/**
 * Equals and hash code implementation for meta node
 */
abstract class AbstractMetaNode<M : MetaNode<M>> : MetaNode<M>, MetaBase()

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
class SealedMeta internal constructor(override val items: Map<NameToken, MetaItem<SealedMeta>>) :
    AbstractMetaNode<SealedMeta>()

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(items.mapValues { entry -> entry.value.seal() })

@Suppress("UNCHECKED_CAST")
fun MetaItem<*>.seal(): MetaItem<SealedMeta> = when (this) {
    is ValueItem -> this
    is NodeItem -> NodeItem(node.seal())
}

object EmptyMeta : MetaBase() {
    override val items: Map<NameToken, MetaItem<*>> = emptyMap()
}

/**
 * Unsafe methods to access values and nodes directly from [MetaItem]
 */
val MetaItem<*>?.value: Value?
    get() = (this as? ValueItem)?.value
        ?: (this?.node?.get(VALUE_KEY) as? ValueItem)?.value

val MetaItem<*>?.string get() = value?.string
val MetaItem<*>?.boolean get() = value?.boolean
val MetaItem<*>?.number get() = value?.number
val MetaItem<*>?.double get() = number?.toDouble()
val MetaItem<*>?.float get() = number?.toFloat()
val MetaItem<*>?.int get() = number?.toInt()
val MetaItem<*>?.long get() = number?.toLong()
val MetaItem<*>?.short get() = number?.toShort()

inline fun <reified E : Enum<E>> MetaItem<*>?.enum() = if (this is ValueItem && this.value is EnumValue<*>) {
    this.value.value as E
} else {
    string?.let { enumValueOf<E>(it) }
}

val MetaItem<*>?.stringList get() = value?.list?.map { it.string } ?: emptyList()

val <M : Meta> MetaItem<M>?.node: M?
    get() = when (this) {
        null -> null
        is ValueItem -> null//error("Trying to interpret value meta item as node item")
        is NodeItem -> node
    }

fun Meta.isEmpty() = this === EmptyMeta || this.items.isEmpty()