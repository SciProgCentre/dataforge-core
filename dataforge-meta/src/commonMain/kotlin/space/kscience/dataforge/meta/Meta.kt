package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.Value


/**
 * The object that could be represented as [Meta]. Meta provided by [toMeta] method should fully represent object state.
 * Meaning that two states with the same meta are equal.
 */
@Serializable(MetaSerializer::class)
public interface MetaRepr {
    public fun toMeta(): Meta
}

/**
 * Generic meta tree representation. Elements are [TypedMetaItem] objects that could be represented by three different entities:
 *  * [MetaItemValue] (leaf)
 *  * [MetaItemNode] single node
 *
 *   * Same name siblings are supported via elements with the same [Name] but different queries
 */
public interface Meta : MetaRepr, ItemProvider {
    /**
     * Top level items of meta tree
     */
    public val items: Map<NameToken, MetaItem>

    override fun getItem(name: Name): MetaItem? {
        if (name.isEmpty()) return MetaItemNode(this)
        return name.firstOrNull()?.let { token ->
            val tail = name.cutFirst()
            when (tail.length) {
                0 -> items[token]
                else -> items[token]?.node?.get(tail)
            }
        }
    }

    override fun toMeta(): Meta = this

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String

    public companion object {
        public const val TYPE: String = "meta"

        /**
         * A key for single value node
         */
        public const val VALUE_KEY: String = "@value"

        public fun equals(meta1: Meta, meta2: Meta): Boolean = meta1.items == meta2.items

        public val EMPTY: Meta = object : MetaBase() {
            override val items: Map<NameToken, MetaItem> = emptyMap()
        }
    }
}

public operator fun Meta.get(token: NameToken): MetaItem? = items.get(token)

/**
 * Get a sequence of [Name]-[Value] pairs
 */
public fun Meta.valueSequence(): Sequence<Pair<Name, Value>> {
    return items.asSequence().flatMap { (key, item) ->
        when (item) {
            is MetaItemValue -> sequenceOf(key.asName() to item.value)
            is MetaItemNode -> item.node.valueSequence().map { pair -> (key.asName() + pair.first) to pair.second }
        }
    }
}

/**
 * Get a sequence of all [Name]-[TypedMetaItem] pairs for all items including nodes
 */
public fun Meta.itemSequence(): Sequence<Pair<Name, MetaItem>> = sequence {
    items.forEach { (key, item) ->
        yield(key.asName() to item)
        if (item is MetaItemNode) {
            yieldAll(item.node.itemSequence().map { (innerKey, innerItem) ->
                (key + innerKey) to innerItem
            })
        }
    }
}

public operator fun Meta.iterator(): Iterator<Pair<Name, MetaItem>> = itemSequence().iterator()

public fun Meta.isEmpty(): Boolean = this === Meta.EMPTY || this.items.isEmpty()