package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.names.*

/**
 * A common parent for [ValueDescriptor] and [NodeDescriptor]. Describes a single [TypedMetaItem] or a group of same-name-siblings.
 */
public sealed interface ItemDescriptor: MetaRepr {

    /**
     * True if same name siblings with this name are allowed
     */
    public val multiple: Boolean

    /**
     * The item description text
     */
    public val info: String?

    /**
     * True if the item is required
     */
    public val required: Boolean


    /**
     * Additional attributes of an item. For example validation and widget parameters
     *
     * @return
     */
    public val attributes: Meta?

    /**
     * An index field by which this node is identified in case of same name siblings construct
     */
    public val indexKey: String

    public companion object {
        public const val DEFAULT_INDEX_KEY: String = "@index"
    }
}


/**
 * The builder for [ItemDescriptor]
 */
@DFBuilder
public sealed class ItemDescriptorBuilder(final override val config: Config) : Configurable, ItemDescriptor {

    /**
     * True if same name siblings with this name are allowed
     */
    override var multiple: Boolean by config.boolean(false)

    /**
     * The item description text
     */
    override var info: String? by config.string()

    /**
     * True if the item is required
     */
    abstract override var required: Boolean


    /**
     * Additional attributes of an item. For example validation and widget parameters
     *
     * @return
     */
    override var attributes: Config? by config.node()

    /**
     * An index field by which this node is identified in case of same name siblings construct
     */
    override var indexKey: String by config.string(DEFAULT_INDEX_KEY)

    public abstract fun build(): ItemDescriptor

    override fun toMeta(): Meta = config

    public companion object {
        public const val DEFAULT_INDEX_KEY: String = "@index"
    }
}

/**
 * Configure attributes of the descriptor, creating an attributes node if needed.
 */
public inline fun ItemDescriptorBuilder.attributes(block: Config.() -> Unit) {
    (attributes ?: Config().also { this.attributes = it }).apply(block)
}

/**
 * Check if given item suits the descriptor
 */
public fun ItemDescriptor.validateItem(item: MetaItem?): Boolean {
    if (item == null) return !required
    return when (this) {
        is ValueDescriptor -> isAllowedValue(item.value ?: return false)
        is NodeDescriptor -> items.all { (key, d) ->
            d.validateItem(item.node[key])
        }
    }
}

/**
 * Get a descriptor item associated with given name or null if item for given name not provided
 */
public operator fun ItemDescriptor.get(name: Name): ItemDescriptor? {
    if (name.isEmpty()) return this
    return when (this) {
        is ValueDescriptor -> null // empty name already checked
        is NodeDescriptor -> items[name.firstOrNull()!!.toString()]?.get(name.cutFirst())
    }
}

public operator fun ItemDescriptor.get(name: String): ItemDescriptor? = get(name.toName())

