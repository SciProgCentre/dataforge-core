package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.EnumValue
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.asValue
import kotlin.js.JsName
import kotlin.jvm.Synchronized


/**
 * Mark a meta builder
 */
@DslMarker
public annotation class MetaBuilder

/**
 * Mutable variant of [Meta]
 * TODO documentation
 */
@Serializable(MutableMetaSerializer::class)
@MetaBuilder
public interface MutableMeta : Meta {

    override val items: Map<NameToken, MutableMeta>

    /**
     * Get or set value of this node
     */
    override var value: Value?

    /**
     * Set or replace node at given [name]
     */
    public operator fun set(name: Name, meta: Meta)

    /**
     * Remove a node at a given [name] if it is present
     */
    public fun remove(name: Name)

    /**
     * Get existing node or create a new one
     */
    public fun getOrCreate(name: Name): MutableMeta

    //TODO to be moved to extensions with multi-receivers

    public infix fun Name.put(value: Value?) {
        set(this, value)
    }

    public infix fun Name.put(string: String) {
        set(this, string.asValue())
    }

    public infix fun Name.put(number: Number) {
        set(this, number.asValue())
    }

    public infix fun Name.put(boolean: Boolean) {
        set(this, boolean.asValue())
    }

    public infix fun Name.put(enum: Enum<*>) {
        set(this, EnumValue(enum))
    }

    public infix fun Name.put(iterable: Iterable<Meta>) {
        setIndexed(this, iterable)
    }

    public infix fun Name.put(meta: Meta) {
        set(this, meta)
    }

    public infix fun Name.put(repr: MetaRepr) {
        put(repr.toMeta())
    }

    public infix fun Name.put(mutableMeta: MutableMeta.() -> Unit) {
        set(this, Meta(mutableMeta))
    }

    public infix fun String.put(meta: Meta) {
        toName() put meta
    }

    public infix fun String.put(value: Value?) {
        set(toName(), value)
    }

    public infix fun String.put(string: String) {
        set(toName(), string.asValue())
    }

    public infix fun String.put(number: Number) {
        set(toName(), number.asValue())
    }

    public infix fun String.put(boolean: Boolean) {
        set(toName(), boolean.asValue())
    }

    public infix fun String.put(enum: Enum<*>) {
        set(toName(), EnumValue(enum))
    }

    public infix fun String.put(array: DoubleArray) {
        set(toName(), array.asValue())
    }

    public infix fun String.put(repr: MetaRepr) {
        toName() put repr.toMeta()
    }

    public infix fun String.put(iterable: Iterable<Meta>) {
        setIndexed(toName(), iterable)
    }

    public infix fun String.put(builder: MutableMeta.() -> Unit) {
        set(toName(), MutableMeta(builder))
    }
}

public fun MutableMeta.getOrCreate(string: String): MutableMeta = getOrCreate(string.toName())

@Serializable(MutableMetaSerializer::class)
public interface MutableTypedMeta<M : MutableTypedMeta<M>> : TypedMeta<M>, MutableMeta {
    /**
     * Zero-copy attach or replace existing node. Node is used with any additional state, listeners, etc.
     * In some cases it is possible to have the same node as a child to several others
     */
    public fun attach(name: Name, node: M)

    override fun getOrCreate(name: Name): M
}

public fun <M : MutableTypedMeta<M>> M.getOrCreate(string: String): M = getOrCreate(string.toName())

public fun MutableMeta.remove(name: String){
    remove(name.toName())
}

// node setters

public operator fun MutableMeta.set(name: NameToken, value: Meta): Unit = set(name.asName(), value)
public operator fun MutableMeta.set(name: Name, meta: Meta?): Unit {
    if (meta == null) {
        remove(name)
    } else {
        set(name, meta)
    }
}

public operator fun MutableMeta.set(key: String, meta: Meta?): Unit {
    set(key.toName(), meta)
}

//value setters

public operator fun MutableMeta.set(name: NameToken, value: Value?): Unit = set(name.asName(), value)
public operator fun MutableMeta.set(key: String, value: Value?): Unit = set(key.toName(), value)

public operator fun MutableMeta.set(name: Name, value: String): Unit = set(name, value.asValue())
public operator fun MutableMeta.set(name: NameToken, value: String): Unit = set(name.asName(), value.asValue())
public operator fun MutableMeta.set(key: String, value: String): Unit = set(key.toName(), value.asValue())

public operator fun MutableMeta.set(name: Name, value: Boolean): Unit = set(name, value.asValue())
public operator fun MutableMeta.set(name: NameToken, value: Boolean): Unit = set(name.asName(), value.asValue())
public operator fun MutableMeta.set(key: String, value: Boolean): Unit = set(key.toName(), value.asValue())

public operator fun MutableMeta.set(name: Name, value: Number): Unit = set(name, value.asValue())
public operator fun MutableMeta.set(name: NameToken, value: Number): Unit = set(name.asName(), value.asValue())
public operator fun MutableMeta.set(key: String, value: Number): Unit = set(key.toName(), value.asValue())

public operator fun MutableMeta.set(name: Name, value: List<Value>): Unit = set(name, value.asValue())
public operator fun MutableMeta.set(name: NameToken, value: List<Value>): Unit = set(name.asName(), value.asValue())
public operator fun MutableMeta.set(key: String, value: List<Value>): Unit = set(key.toName(), value.asValue())

//public fun MutableMeta.set(key: String, index: String, value: Value?): Unit =
//    set(key.toName().withIndex(index), value)


/**
 * Universal unsafe set method
 */
public operator fun MutableMeta.set(name: Name, value: Value?) {
    getOrCreate(name).value = Value.of(value)
}

/* Same name siblings generation */

public fun MutableMeta.setIndexedItems(
    name: Name,
    items: Iterable<Meta>,
    indexFactory: (Meta, index: Int) -> String = { _, index -> index.toString() },
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    items.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, (last.index ?: "") + indexFactory(meta, index))
        tokens[tokens.lastIndex] = indexedToken
        set(Name(tokens), meta)
    }
}

public fun MutableMeta.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    indexFactory: (Meta, index: Int) -> String = { _, index -> index.toString() },
) {
    setIndexedItems(name, metas) { item, index -> indexFactory(item, index) }
}

public operator fun MutableMeta.set(name: Name, metas: Iterable<Meta>): Unit =
    setIndexed(name, metas)

public operator fun MutableMeta.set(name: String, metas: Iterable<Meta>): Unit =
    setIndexed(name.toName(), metas)


/**
 * Update existing mutable node with another node. The rules are following:
 *  * value replaces anything
 *  * node updates node and replaces anything but node
 *  * node list updates node list if number of nodes in the list is the same and replaces anything otherwise
 */
public fun MutableMeta.update(meta: Meta) {
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

/**
 * A general implementation of mutable [Meta] which implements both [MutableTypedMeta] and [ObservableMeta].
 * The implementation uses blocking synchronization on mutation on JVM
 */
private class MutableMetaImpl(
    value: Value?,
    children: Map<NameToken, Meta> = emptyMap()
) : ObservableMutableMeta {

    override var value = value
        @Synchronized set

    private val children: LinkedHashMap<NameToken, ObservableMutableMeta> =
        LinkedHashMap(children.mapValues { (key, meta) ->
            MutableMetaImpl(meta.value, meta.items).apply { adoptBy(this, key) }
        })

    override val items: Map<NameToken, ObservableMutableMeta> get() = children

    private val listeners = HashSet<MetaListener>()

    private fun changed(name: Name) {
        listeners.forEach { it.callback(this, name) }
    }

    @Synchronized
    override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
        listeners.add(MetaListener(owner, callback))
    }

    @Synchronized
    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }


    private fun ObservableMeta.adoptBy(parent: MutableMetaImpl, key: NameToken) {
        onChange(parent) { name ->
            parent.changed(key + name)
        }
    }

    override fun attach(name: Name, node: ObservableMutableMeta) {
        when (name.length) {
            0 -> error("Can't set a meta with empty name")
            1 -> {
                replaceItem(name.first(), get(name), node)
            }
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

    override fun getOrCreate(name: Name): ObservableMutableMeta = get(name) ?: createNode(name)

    override fun remove(name: Name) {
        when (name.length) {
            0 -> error("Can't remove self")
            1 -> if (children.remove(name.firstOrNull()!!) != null) changed(name)
            else -> get(name.cutLast())?.remove(name.lastOrNull()!!.asName())
        }
    }

    @Synchronized
    private fun replaceItem(
        key: NameToken,
        oldItem: ObservableMutableMeta?,
        newItem: ObservableMutableMeta?
    ) {
        if (oldItem != newItem) {
            if (newItem == null) {
                //remove child and remove stale listener
                children.remove(key)?.removeListener(this)
            } else {
                newItem.adoptBy(this, key)
                children[key] = newItem
            }
            changed(key.asName())
        }
    }

    private fun wrapItem(meta: Meta): MutableMetaImpl =
        MutableMetaImpl(meta.value, meta.items.mapValuesTo(LinkedHashMap()) { wrapItem(it.value) })


    override fun set(name: Name, meta: Meta) {
        val oldItem: ObservableMutableMeta? = get(name)
        if (oldItem != meta) {
            when (name.length) {
                0 -> error("Can't set a meta with empty name")
                1 -> {
                    val token = name.firstOrNull()!!
                    replaceItem(token, oldItem, wrapItem(meta))
                }
                else -> {
                    val token = name.firstOrNull()!!
                    //get existing or create new node. Index is ignored for new node
                    if (items[token] == null) {
                        replaceItem(token, null, MutableMetaImpl(null))
                    }
                    items[token]?.set(name.cutFirst(), meta)
                }
            }
            changed(name)
        }
    }

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}

/**
 * Append the node with a same-name-sibling, automatically generating numerical index
 */
@DFExperimental
public fun MutableMeta.append(name: Name, value: Value?) {
    require(!name.isEmpty()) { "Name could not be empty for append operation" }
    val newIndex = name.lastOrNull()!!.index
    if (newIndex != null) {
        set(name, value)
    } else {
        val index = (getIndexed(name).keys.mapNotNull { it?.toIntOrNull() }.maxOrNull() ?: -1) + 1
        set(name.withIndex(index.toString()), value)
    }
}

@DFExperimental
public fun MutableMeta.append(name: String, value: Value?): Unit = append(name.toName(), value)

///**
// * Apply existing node with given [builder] or create a new element with it.
// */
//@DFExperimental
//public fun MutableMeta.edit(name: Name, builder: MutableMeta.() -> Unit) {
//    val item = when (val existingItem = get(name)) {
//        null -> MutableMeta().also { set(name, it) }
//        is MetaItemNode<MutableMeta> -> existingItem.node
//        else -> error("Can't edit value meta item")
//    }
//    item.apply(builder)
//}

/**
 * Create a mutable copy of this meta. The copy is created even if the Meta is already mutable
 */
public fun Meta.toMutableMeta(): ObservableMutableMeta = MutableMetaImpl(value, items)

public fun Meta.asMutableMeta(): MutableMeta = (this as? MutableMeta) ?: toMutableMeta()

@JsName("newMutableMeta")
public fun MutableMeta(): ObservableMutableMeta = MutableMetaImpl(null)

/**
 * Build a [MutableMeta] using given transformation
 */
@Suppress("FunctionName")
public inline fun MutableMeta(builder: MutableMeta.() -> Unit = {}): ObservableMutableMeta =
    MutableMeta().apply(builder)


/**
 * Create a copy of this [Meta], optionally applying the given [block].
 * The listeners of the original Config are not retained.
 */
public inline fun Meta.copy(block: MutableMeta.() -> Unit = {}): Meta =
    toMutableMeta().apply(block)


private class MutableMetaWithDefault(val source: MutableMeta, val default: Meta, val name: Name) :
    MutableMeta by source {
    override val items: Map<NameToken, MutableMeta>
        get() = (source.items.keys + default.items.keys).associateWith {
            MutableMetaWithDefault(source, default, name + it)
        }

    override var value: Value?
        get() = source[name]?.value ?: default[name]?.value
        set(value) {
            source[name] = value
        }

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}

/**
 * Create a mutable item provider that uses given provider for default values if those are not found in this provider.
 * Changes are propagated only to this provider.
 */
public fun MutableMeta.withDefault(default: Meta?): MutableMeta = if (default == null || default.isEmpty()) {
    //Optimize for use with empty default
    this
} else MutableMetaWithDefault(this, default, Name.EMPTY)
