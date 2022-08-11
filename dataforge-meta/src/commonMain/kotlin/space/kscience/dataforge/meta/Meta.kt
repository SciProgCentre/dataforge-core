package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.misc.unsafeCast
import space.kscience.dataforge.names.*


/**
 * The object that could be represented as [Meta]. Meta provided by [toMeta] method should fully represent object state.
 * Meaning that two states with the same meta are equal.
 */

public interface MetaRepr {
    public fun toMeta(): Meta
}

/**
 * A container for meta nodes
 */
public fun interface MetaProvider : ValueProvider {
    public fun getMeta(name: Name): Meta?

    override fun getValue(name: Name): Value? = getMeta(name)?.value
}

/**
 * A meta node
 * TODO add documentation
 * Same name siblings are supported via elements with the same [Name] but different indices.
 */
@Type(Meta.TYPE)
@Serializable(MetaSerializer::class)
public interface Meta : MetaRepr, MetaProvider {
    public val value: Value?
    public val items: Map<NameToken, Meta>

    override fun getMeta(name: Name): Meta? {
        tailrec fun Meta.find(name: Name): Meta? = if (name.isEmpty()) {
            this
        } else {
            items[name.firstOrNull()!!]?.find(name.cutFirst())
        }

        return find(name)
    }

    override fun toMeta(): Meta = this

    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    public companion object {
        public const val TYPE: String = "meta"

        /**
         * A key for single value node
         */
        public const val VALUE_KEY: String = "@value"
        public const val INDEX_KEY: String = "@index"

        public fun hashCode(meta: Meta): Int {
            var result = meta.value?.hashCode() ?: 0
            result = 31 * result + meta.items.hashCode()
            return result
        }

        public fun equals(meta1: Meta?, meta2: Meta?): Boolean {
            if (meta1 == null && meta2 == null) return true
            if (meta1 == null || meta2 == null) return false
            if (meta1.value != meta2.value) return false
            if (meta1.items.keys != meta2.items.keys) return false
            return meta1.items.keys.all {
                equals(meta1[it], meta2[it])
            }
        }

        private val json = Json {
            prettyPrint = true
            useArrayPolymorphism = true
        }

        public fun toString(meta: Meta): String = json.encodeToString(MetaSerializer, meta)

        public val EMPTY: Meta = SealedMeta(null, emptyMap())
    }
}

/**
 * True if this [Meta] does not have children
 */
public val Meta.isLeaf: Boolean get() = items.isEmpty()


public operator fun Meta.get(token: NameToken): Meta? = items[token]

/**
 * Perform recursive item search using given [name]. Each [NameToken] is treated as a name in [Meta.items] of a parent node.
 *
 * If [name] is empty return current [Meta]
 */
public operator fun Meta.get(name: Name): Meta? = this.getMeta(name)

//TODO allow nullable receivers after Kotlin 1.7

/**
 * Parse [Name] from [key] using full name notation and pass it to [Meta.get]
 */
public operator fun Meta.get(key: String): Meta? = this[Name.parse(key)]

/**
 * Get all items matching given name. The index of the last element, if present is used as a [Regex],
 * against which indexes of elements are matched.
 */
public fun Meta.getIndexed(name: Name): Map<String?, Meta> {
    val root: Meta = when (name.length) {
        0 -> error("Can't use empty name for 'getIndexed'")
        1 -> this
        else -> this[name.cutLast()] ?: return emptyMap()
    }

    val (body, index) = name.lastOrNull()!!
    return if (index == null) {
        root.items
            .filter { it.key.body == body }
            .mapKeys { it.key.index }
    } else {
        val regex = index.toRegex()
        root.items
            .filter { it.key.body == body && (regex.matches(it.key.index ?: "")) }
            .mapKeys { it.key.index }
    }
}

public fun Meta.getIndexed(name: String): Map<String?, Meta>  = getIndexed(name.parseAsName())

/**
 * A meta node that ensures that all of its descendants has at least the same type.
 *
 */
public interface TypedMeta<out M : TypedMeta<M>> : Meta {

    override val items: Map<NameToken, M>

    override fun getMeta(name: Name): M? {
        tailrec fun M.find(name: Name): M? = if (name.isEmpty()) {
            this
        } else {
            items[name.firstOrNull()!!]?.find(name.cutFirst())
        }

        return self.find(name)
    }

    override fun toMeta(): Meta = this
}

/**
 * Access self as a recursive type instance
 */
public inline val <M : TypedMeta<M>> TypedMeta<M>.self: M get() = unsafeCast()

//public typealias Meta = TypedMeta<*>

public operator fun <M : TypedMeta<M>> TypedMeta<M>.get(token: NameToken): M? = items[token]

/**
 * Perform recursive item search using given [name]. Each [NameToken] is treated as a name in [TypedMeta.items] of a parent node.
 *
 * If [name] is empty return current [Meta]
 */
public tailrec operator fun <M : TypedMeta<M>> TypedMeta<M>.get(name: Name): M? = if (name.isEmpty()) {
    self
} else {
    get(name.firstOrNull()!!)?.get(name.cutFirst())
}

/**
 * Parse [Name] from [key] using full name notation and pass it to [TypedMeta.get]
 */
public operator fun <M : TypedMeta<M>> TypedMeta<M>.get(key: String): M? = this[Name.parse(key)]


/**
 * Get a sequence of [Name]-[Value] pairs using top-down traversal of the tree
 */
public fun Meta.valueSequence(): Sequence<Pair<Name, Value>> = sequence {
    items.forEach { (key, item) ->
        item.value?.let { itemValue ->
            yield(key.asName() to itemValue)
        }
        yieldAll(item.valueSequence().map { pair -> (key.asName() + pair.first) to pair.second })
    }
}

/**
 * Get a sequence of all [Name]-[TypedMeta] pairs in a top-down traversal
 */
public fun Meta.nodeSequence(): Sequence<Pair<Name, Meta>> = sequence {
    items.forEach { (key, item) ->
        yield(key.asName() to item)
        yieldAll(item.nodeSequence().map { (innerKey, innerItem) ->
            (key + innerKey) to innerItem
        })
    }
}

public operator fun Meta.iterator(): Iterator<Pair<Name, Meta>> = nodeSequence().iterator()

public fun Meta.isEmpty(): Boolean = this === Meta.EMPTY
        || (value == null && (items.isEmpty() || items.values.all { it.isEmpty() }))


/* Get operations*/

/**
 * Get all items matching given name. The index of the last element, if present is used as a [Regex],
 * against which indexes of elements are matched.
 */
@Suppress("UNCHECKED_CAST")
public fun <M : TypedMeta<M>> TypedMeta<M>.getIndexed(name: Name): Map<String?, M> =
    (this as Meta).getIndexed(name) as Map<String?, M>

public fun <M : TypedMeta<M>> TypedMeta<M>.getIndexed(name: String): Map<String?, Meta> = getIndexed(Name.parse(name))


public val Meta?.string: String? get() = this?.value?.string
public val Meta?.boolean: Boolean? get() = this?.value?.boolean
public val Meta?.number: Number? get() = this?.value?.numberOrNull
public val Meta?.double: Double? get() = number?.toDouble()
public val Meta?.float: Float? get() = number?.toFloat()
public val Meta?.int: Int? get() = number?.toInt()
public val Meta?.long: Long? get() = number?.toLong()
public val Meta?.short: Short? get() = number?.toShort()

public inline fun <reified E : Enum<E>> Meta?.enum(): E? = this?.value?.let {
    if (it is EnumValue<*>) {
        it.value as E
    } else {
        string?.let { str -> enumValueOf<E>(str) }
    }
}

public val Meta.stringList: List<String>? get() = value?.list?.map { it.string }

/**
 * Create a provider that uses given provider for default values if those are not found in this provider
 */
public fun Meta.withDefault(default: MetaProvider?): Meta = if (default == null) {
    this
} else {
    //TODO optimize
    toMutableMeta().withDefault(default)
}