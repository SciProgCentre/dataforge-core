package hep.dataforge.meta

import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.meta.MetaItem.ValueItem
import hep.dataforge.names.*
import hep.dataforge.values.Value
import kotlinx.serialization.json.Json


/**
 * The object that could be represented as [Meta]. Meta provided by [toMeta] method should fully represent object state.
 * Meaning that two states with the same meta are equal.
 */
@Serializable(MetaSerializer::class)
public interface MetaRepr {
    public fun toMeta(): Meta
}

/**
 * Generic meta tree representation. Elements are [MetaItem] objects that could be represented by three different entities:
 *  * [MetaItem.ValueItem] (leaf)
 *  * [MetaItem.NodeItem] single node
 *
 *   * Same name siblings are supported via elements with the same [Name] but different queries
 */
public interface Meta : MetaRepr, ItemProvider {
    /**
     * Top level items of meta tree
     */
    public val items: Map<NameToken, MetaItem<*>>

    override fun getItem(name: Name): MetaItem<*>? {
        if (name.isEmpty()) return NodeItem(this)
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

        public val EMPTY: Meta = object : MetaBase() {
            override val items: Map<NameToken, MetaItem<*>> = emptyMap()
        }
    }
}

public operator fun Meta?.get(token: NameToken): MetaItem<*>? = this?.items?.get(token)

/**
 * Get a sequence of [Name]-[Value] pairs
 */
public fun Meta.valueSequence(): Sequence<Pair<Name, Value>> {
    return items.asSequence().flatMap { (key, item) ->
        when (item) {
            is ValueItem -> sequenceOf(key.asName() to item.value)
            is NodeItem -> item.node.valueSequence().map { pair -> (key.asName() + pair.first) to pair.second }
        }
    }
}

/**
 * Get a sequence of all [Name]-[MetaItem] pairs for all items including nodes
 */
public fun Meta.itemSequence(): Sequence<Pair<Name, MetaItem<*>>> = sequence {
    items.forEach { (key, item) ->
        yield(key.asName() to item)
        if (item is NodeItem<*>) {
            yieldAll(item.node.itemSequence().map { (innerKey, innerItem) ->
                (key + innerKey) to innerItem
            })
        }
    }
}

public operator fun Meta.iterator(): Iterator<Pair<Name, MetaItem<*>>> = itemSequence().iterator()

/**
 * A meta node that ensures that all of its descendants has at least the same type
 */
public interface TypedMeta<out M : TypedMeta<M>> : Meta {
    override val items: Map<NameToken, MetaItem<M>>
}

/**
 * The same as [Meta.get], but with specific node type
 */
public operator fun <M : TypedMeta<M>> M?.get(name: Name): MetaItem<M>? = if (this == null) {
    null
} else {
    @Suppress("UNCHECKED_CAST", "ReplaceGetOrSet")
    (this as Meta).get(name) as MetaItem<M>? // Do not change
}

public operator fun <M : TypedMeta<M>> M?.get(key: String): MetaItem<M>? = this[key.toName()]
public operator fun <M : TypedMeta<M>> M?.get(key: NameToken): MetaItem<M>? = this[key.asName()]

/**
 * Equals, hashcode and to string for any meta
 */
public abstract class MetaBase : Meta {

    override fun equals(other: Any?): Boolean = if (other is Meta) {
        this.items == other.items
    } else {
        false
    }

    override fun hashCode(): Int = items.hashCode()

    override fun toString(): String = Json {
        prettyPrint = true
        useArrayPolymorphism = true
    }.encodeToString(MetaSerializer, this)
}

/**
 * Equals and hash code implementation for meta node
 */
public abstract class AbstractTypedMeta<M : TypedMeta<M>> : TypedMeta<M>, MetaBase()

public fun Meta.isEmpty(): Boolean = this === Meta.EMPTY || this.items.isEmpty()