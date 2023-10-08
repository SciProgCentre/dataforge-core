package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.ThreadSafe
import space.kscience.dataforge.names.*
import kotlin.js.JsName


/**
 * Mark a meta builder
 */
@DslMarker
public annotation class MetaBuilderMarker

/**
 * A generic interface that gives access to getting and setting meta notes and values
 */
public interface MutableMetaProvider : MetaProvider, MutableValueProvider {
    override fun getMeta(name: Name): MutableMeta?
    public fun setMeta(name: Name, node: Meta?)
    override fun setValue(name: Name, value: Value?)
}

/**
 * Mutable variant of [Meta]
 * TODO documentation
 */
@Serializable(MutableMetaSerializer::class)
@MetaBuilderMarker
public interface MutableMeta : Meta, MutableMetaProvider {

    override val items: Map<NameToken, MutableMeta>

    /**
     * Get or set value of this node
     */
    override var value: Value?

    override fun getMeta(name: Name): MutableMeta? {
        tailrec fun MutableMeta.find(name: Name): MutableMeta? = if (name.isEmpty()) {
            this
        } else {
            items[name.firstOrNull()!!]?.find(name.cutFirst())
        }

        return find(name)
    }

    override fun setValue(name: Name, value: Value?) {
        getOrCreate(name).value = value
    }

    /**
     * Get existing node or create a new one
     */
    public fun getOrCreate(name: Name): MutableMeta

    //TODO to be moved to extensions with multi-receivers

    public infix fun Name.put(value: Value?) {
        setValue(this, value)
    }

    public infix fun Name.put(string: String) {
        setValue(this, string.asValue())
    }

    public infix fun Name.put(number: Number) {
        setValue(this, number.asValue())
    }

    public infix fun Name.put(boolean: Boolean) {
        setValue(this, boolean.asValue())
    }

    public infix fun Name.put(enum: Enum<*>) {
        setValue(this, EnumValue(enum))
    }

    public infix fun Name.putIndexed(iterable: Iterable<Meta>) {
        setIndexed(this, iterable)
    }

    public infix fun Name.put(meta: Meta) {
        setMeta(this, meta)
    }

    public infix fun Name.put(repr: MetaRepr) {
        setMeta(this, repr.toMeta())
    }

    public infix fun Name.put(builder: MutableMeta.() -> Unit) {
        getOrCreate(this).apply(builder)
    }

    public infix fun String.put(meta: Meta) {
        setMeta(Name.parse(this), meta)
    }

    public infix fun String.put(value: Value?) {
        setValue(Name.parse(this), value)
    }

    public infix fun String.put(string: String) {
        setValue(Name.parse(this), string.asValue())
    }

    public infix fun String.put(number: Number) {
        setValue(Name.parse(this), number.asValue())
    }

    public infix fun String.put(boolean: Boolean) {
        setValue(Name.parse(this), boolean.asValue())
    }

    public infix fun String.put(enum: Enum<*>) {
        setValue(Name.parse(this), EnumValue(enum))
    }

    public infix fun String.put(array: DoubleArray) {
        setValue(Name.parse(this), array.asValue())
    }

    public infix fun String.put(repr: MetaRepr) {
        setMeta(Name.parse(this), repr.toMeta())
    }

    public infix fun String.putIndexed(iterable: Iterable<Meta>) {
        setIndexed(Name.parse(this), iterable)
    }

    public infix fun String.put(builder: MutableMeta.() -> Unit) {
        getOrCreate(parseAsName()).apply(builder)
    }
}

/**
 * Set or replace node at given [name]
 */
public operator fun MutableMetaProvider.set(name: Name, meta: Meta): Unit = setMeta(name, meta)

/**
 * Set or replace value at given [name]
 */
public operator fun MutableValueProvider.set(name: Name, value: Value?): Unit = setValue(name, value)

public fun MutableMeta.getOrCreate(key: String): MutableMeta = getOrCreate(Name.parse(key))

public interface MutableTypedMeta<M : MutableTypedMeta<M>> : TypedMeta<M>, MutableMeta {
    /**
     * Zero-copy (if possible) attach or replace existing node. Node is used with any additional state, listeners, etc.
     * In some cases it is possible to have the same node as a child to several others
     */
    @DFExperimental
    public fun attach(name: Name, node: M)
    override fun getMeta(name: Name): M?
    override fun getOrCreate(name: Name): M
}

public fun <M : MutableTypedMeta<M>> M.getOrCreate(key: String): M = getOrCreate(Name.parse(key))

public fun MutableMetaProvider.remove(name: Name) {
    setMeta(name, null)
}

public fun MutableMetaProvider.remove(key: String) {
    setMeta(Name.parse(key), null)
}

// node setters

public operator fun MutableMetaProvider.set(Key: NameToken, value: Meta): Unit = setMeta(Key.asName(), value)
public operator fun MutableMetaProvider.set(key: String, value: Meta): Unit = setMeta(Name.parse(key), value)


//public fun MutableMeta.set(key: String, index: String, value: Value?): Unit =
//    set(key.toName().withIndex(index), value)


/* Same name siblings generation */


public fun MutableMetaProvider.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    indexFactory: (Meta, index: Int) -> String = { _, index -> index.toString() },
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    metas.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, (last.index ?: "") + indexFactory(meta, index))
        tokens[tokens.lastIndex] = indexedToken
        set(Name(tokens), meta)
    }
}

public operator fun MutableMetaProvider.set(name: Name, metas: Iterable<Meta>): Unit =
    setIndexed(name, metas)

public operator fun MutableMetaProvider.set(key: String, metas: Iterable<Meta>): Unit =
    setIndexed(Name.parse(key), metas)


/**
 * Update existing mutable node with another node. The rules are following:
 *  * value replaces anything
 *  * node updates node and replaces anything but node
 *  * node list updates node list if number of nodes in the list is the same and replaces anything otherwise
 */
public fun MutableMetaProvider.update(meta: Meta) {
    meta.valueSequence().forEach { (name, value) ->
        set(name, value)
    }
}

///**
// * Get child with given name or create a new one
// */
//public fun <M : MutableTypedMeta<M>> MutableTypedMeta<M>.getOrCreate(name: Name): M =
//    get(name) ?: empty().also { attach(name, it) }

/**
 * Edit node at [name]
 */
public fun <M : MutableTypedMeta<M>> MutableTypedMeta<M>.edit(name: Name, builder: M.() -> Unit): M =
    getOrCreate(name).apply(builder)

/**
 * Set a value at a given [name]. If node does not exist, create it.
 */
public operator fun <M : MutableTypedMeta<M>> MutableTypedMeta<M>.set(name: Name, value: Value?) {
    edit(name) {
        this.value = value
    }
}

private fun ObservableMeta.adoptBy(parent: MutableMetaImpl, key: NameToken) {
    if (this === parent) error("Can't attach a node to itself")
    onChange(parent) { name ->
        parent.invalidate(key + name)
    }
}

/**
 * A general implementation of mutable [Meta] which implements both [MutableTypedMeta] and [ObservableMeta].
 * The implementation uses blocking synchronization on mutation on JVM
 */
private class MutableMetaImpl(
    value: Value?,
    children: Map<NameToken, Meta> = emptyMap(),
) : AbstractObservableMeta(), ObservableMutableMeta {
    override var value = value
        @ThreadSafe set(value) {
            val oldValue = field
            field = value
            if (oldValue != value) {
                invalidate(Name.EMPTY)
            }
        }

    private val children: LinkedHashMap<NameToken, ObservableMutableMeta> =
        LinkedHashMap(children.mapValues { (key, meta) ->
            MutableMetaImpl(meta.value, meta.items).also { it.adoptBy(this, key) }
        })

    override val items: Map<NameToken, ObservableMutableMeta> get() = children

    @DFExperimental
    override fun attach(name: Name, node: ObservableMutableMeta) {
        when (name.length) {
            0 -> error("Can't set a meta with empty name")
            1 -> replaceItem(name.first(), get(name), node)
            else -> get(name.cutLast())?.attach(name.lastOrNull()!!.asName(), node)
        }
    }

    /**
     * Create and attach empty node
     */
    private fun createNode(name: Name): ObservableMutableMeta = when (name.length) {
        0 -> throw IllegalArgumentException("Can't create a node with empty name")
        1 -> {
            val newNode = MutableMetaImpl(null)
            children[name.first()] = newNode
            newNode.adoptBy(this, name.first())
            newNode
        } //do not notify, no value changed
        else -> getOrCreate(name.first().asName()).getOrCreate(name.cutFirst())
    }

    override fun getOrCreate(name: Name): ObservableMutableMeta =
        if (name.isEmpty()) this else get(name) ?: createNode(name)

    @ThreadSafe
    private fun replaceItem(
        key: NameToken,
        oldItem: ObservableMutableMeta?,
        newItem: ObservableMutableMeta?,
    ) {
        if (oldItem != newItem) {
            if (newItem == null) {
                //remove child and remove stale listener
                children.remove(key)?.removeListener(this)
            } else {
                newItem.adoptBy(this, key)
                children[key] = newItem
            }
            invalidate(key.asName())
        }
    }

    private fun wrapItem(meta: Meta): MutableMetaImpl =
        meta as? MutableMetaImpl ?: MutableMetaImpl(
            meta.value,
            meta.items.mapValuesTo(LinkedHashMap()) {
                wrapItem(it.value)
            }
        )

    @ThreadSafe
    override fun setMeta(name: Name, node: Meta?) {
        val oldItem: ObservableMutableMeta? = get(name)
        if (oldItem != node) {
            when (name.length) {
                0 -> error("Can't set a meta with empty name")
                1 -> {
                    val token = name.firstOrNull()!!
                    //remove child and invalidate if argument is null
                    if (node == null) {
                        children.remove(token)?.removeListener(this)
                        // old item is not null otherwise we can't be here
                        invalidate(name)
                    } else {
                        val newNode = wrapItem(node)
                        newNode.adoptBy(this, token)
                        children[token] = newNode
                    }
                }

                else -> {
                    val token = name.firstOrNull()!!
                    //get existing or create new node.
                    if (items[token] == null) {
                        val newNode = MutableMetaImpl(null)
                        newNode.adoptBy(this, token)
                        children[token] = newNode
                    }
                    items[token]?.setMeta(name.cutFirst(), node)
                }
            }
            invalidate(name)
        }
    }
}

/**
 * Append the node with a same-name-sibling, automatically generating numerical index
 */
public fun MutableMeta.append(name: Name, meta: Meta) {
    require(!name.isEmpty()) { "Name could not be empty for append operation" }
    val newIndex = name.lastOrNull()!!.index
    if (newIndex != null) {
        set(name, meta)
    } else {
        val index = (getIndexed(name).keys.mapNotNull { it?.toIntOrNull() }.maxOrNull() ?: -1) + 1
        set(name.withIndex(index.toString()), meta)
    }
}

public fun MutableMeta.append(key: String, meta: Meta): Unit = append(Name.parse(key), meta)

public fun MutableMeta.append(name: Name, value: Value): Unit = append(name, Meta(value))

public fun MutableMeta.append(key: String, value: Value): Unit = append(Name.parse(key), value)

/**
 * Create a mutable copy of this meta. The copy is created even if the Meta is already mutable
 */
public fun Meta.toMutableMeta(): ObservableMutableMeta = MutableMetaImpl(value, items)

public fun Meta.asMutableMeta(): MutableMeta = (this as? MutableMeta) ?: toMutableMeta()

@JsName("newObservableMutableMeta")
public fun ObservableMutableMeta(): ObservableMutableMeta = MutableMetaImpl(null)

/**
 * Build a [MutableMeta] using given transformation
 */
public inline fun ObservableMutableMeta(builder: MutableMeta.() -> Unit = {}): ObservableMutableMeta =
    ObservableMutableMeta().apply(builder)


/**
 * Create a copy of this [Meta], optionally applying the given [block].
 * The listeners of the original Config are not retained.
 */
public inline fun Meta.copy(block: MutableMeta.() -> Unit = {}): Meta =
    toMutableMeta().apply(block)


private class MutableMetaWithDefault(
    val source: MutableMeta, val default: MetaProvider, val rootName: Name,
) : MutableMeta by source {
    override val items: Map<NameToken, MutableMeta>
        get() {
            val sourceKeys: Collection<NameToken> = source[rootName]?.items?.keys ?: emptyList()
            val defaultKeys: Collection<NameToken> = default.getMeta(rootName)?.items?.keys ?: emptyList()
            //merging keys for primary and default node
            return (sourceKeys + defaultKeys).associateWith {
                MutableMetaWithDefault(source, default, rootName + it)
            }
        }

    override var value: Value?
        get() = source[rootName]?.value ?: default.getMeta(rootName)?.value
        set(value) {
            source[rootName] = value
        }

    override fun getMeta(name: Name): MutableMeta = MutableMetaWithDefault(source, default, rootName + name)

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}

/**
 * Create a mutable item provider that uses given provider for default values if those are not found in this provider.
 * Changes are propagated only to this provider.
 */
public fun MutableMeta.withDefault(default: MetaProvider?): MutableMeta = if (default == null) {
    this
} else MutableMetaWithDefault(this, default, Name.EMPTY)
