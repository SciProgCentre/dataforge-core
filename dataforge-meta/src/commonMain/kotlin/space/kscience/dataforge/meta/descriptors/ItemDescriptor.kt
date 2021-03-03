package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.*

/**
 * A common parent for [ValueDescriptor] and [NodeDescriptor]. Describes a single [TypedMetaItem] or a group of same-name-siblings.
 */
@DFBuilder
public sealed class ItemDescriptor(final override val config: Config) : Configurable {

    /**
     * True if same name siblings with this name are allowed
     */
    public var multiple: Boolean by config.boolean(false)

    /**
     * The item description text
     */
    public var info: String? by config.string()

    /**
     * True if the item is required
     */
    public abstract var required: Boolean


    /**
     * Additional attributes of an item. For example validation and widget parameters
     *
     * @return
     */
    public var attributes: Config? by config.node()

    /**
     * An index field by which this node is identified in case of same name siblings construct
     */
    public var indexKey: String by config.string(DEFAULT_INDEX_KEY)

    public abstract fun copy(): ItemDescriptor

    public companion object {
        public const val DEFAULT_INDEX_KEY: String = "@index"
    }
}

/**
 * Configure attributes of the descriptor, creating an attributes node if needed.
 */
public inline fun ItemDescriptor.attributes(block: Config.() -> Unit) {
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
 * Descriptor for meta node. Could contain additional information for viewing
 * and editing.
 *
 * @author Alexander Nozik
 */
@DFBuilder
public class NodeDescriptor(config: Config = Config()) : ItemDescriptor(config) {
    init {
        config[IS_NODE_KEY] = true
    }

    /**
     * True if the node is required
     *
     * @return
     */
    override var required: Boolean by config.boolean { default == null }

    /**
     * The default for this node. Null if there is no default.
     *
     * @return
     */
    public var default: Config? by config.node()

    /**
     * The map of children item descriptors (both nodes and values)
     */
    public val items: Map<String, ItemDescriptor>
        get() = config.getIndexed(ITEM_KEY).entries.associate { (name, item) ->
            if (name == null) error("Child item index should not be null")
            val node = item.node ?: error("Node descriptor must be a node")
            if (node[IS_NODE_KEY].boolean == true) {
                name to NodeDescriptor(node as Config)
            } else {
                name to ValueDescriptor(node as Config)
            }
        }

    /**
     * The map of children node descriptors
     */
    @Suppress("UNCHECKED_CAST")
    public val nodes: Map<String, NodeDescriptor>
        get() = config.getIndexed(ITEM_KEY).entries.filter {
            it.value.node[IS_NODE_KEY].boolean == true
        }.associate { (name, item) ->
            if (name == null) error("Child node index should not be null")
            val node = item.node ?: error("Node descriptor must be a node")
            name to NodeDescriptor(node as Config)
        }

    /**
     * The list of children value descriptors
     */
    public val values: Map<String, ValueDescriptor>
        get() = config.getIndexed(ITEM_KEY).entries.filter {
            it.value.node[IS_NODE_KEY].boolean != true
        }.associate { (name, item) ->
            if (name == null) error("Child value index should not be null")
            val node = item.node ?: error("Node descriptor must be a node")
            name to ValueDescriptor(node as Config)
        }

    private fun buildNode(name: Name): NodeDescriptor {
        return when (name.length) {
            0 -> this
            1 -> {
                val token = NameToken(ITEM_KEY.toString(), name.toString())
                val config: Config = config[token].node ?: Config().also {
                    it[IS_NODE_KEY] = true
                    config[token] = it
                }
                NodeDescriptor(config)
            }
            else -> buildNode(name.firstOrNull()?.asName()!!).buildNode(name.cutFirst())
        }
    }

    /**
     * Define a child item descriptor for this node
     */
    private fun newItem(key: String, descriptor: ItemDescriptor) {
        if (items.keys.contains(key)) error("The key $key already exists in descriptor")
        val token = ITEM_KEY.withIndex(key)
        config[token] = descriptor.config
    }

    public fun item(name: Name, descriptor: ItemDescriptor) {
        buildNode(name.cutLast()).newItem(name.lastOrNull().toString(), descriptor)
    }

    public fun item(name: String, descriptor: ItemDescriptor) {
        item(name.toName(), descriptor)
    }

    /**
     * Create and configure a child node descriptor
     */
    public fun node(name: Name, block: NodeDescriptor.() -> Unit) {
        item(name, NodeDescriptor().apply(block))
    }

    public fun node(name: String, block: NodeDescriptor.() -> Unit) {
        node(name.toName(), block)
    }

    /**
     * Create and configure child value descriptor
     */
    public fun value(name: Name, block: ValueDescriptor.() -> Unit) {
        require(name.length >= 1) { "Name length for value descriptor must be non-empty" }
        item(name, ValueDescriptor().apply(block))
    }

    public fun value(name: String, block: ValueDescriptor.() -> Unit) {
        value(name.toName(), block)
    }

    override fun copy(): NodeDescriptor = NodeDescriptor(config.toConfig())

    public companion object {

        internal val ITEM_KEY: Name = "item".asName()
        internal val IS_NODE_KEY: Name = "@isNode".asName()

        //TODO infer descriptor from spec
    }
}

public inline fun NodeDescriptor(block: NodeDescriptor.() -> Unit): NodeDescriptor =
    NodeDescriptor().apply(block)

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

/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
@DFBuilder
public class ValueDescriptor(config: Config = Config()) : ItemDescriptor(config) {

    /**
     * True if the value is required
     *
     * @return
     */
    override var required: Boolean by config.boolean { default == null }

    /**
     * The default for this value. Null if there is no default.
     *
     * @return
     */
    public var default: Value? by config.value()

    public fun default(v: Any) {
        this.default = Value.of(v)
    }

    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    public var type: List<ValueType>? by config.listValue { ValueType.valueOf(it.string) }

    public fun type(vararg t: ValueType) {
        this.type = listOf(*t)
    }

    /**
     * Check if given value is allowed for here. The type should be allowed and
     * if it is value should be within allowed values
     *
     * @param value
     * @return
     */
    public fun isAllowedValue(value: Value): Boolean {
        return (type?.let { it.contains(ValueType.STRING) || it.contains(value.type) } ?: true)
                && (allowedValues.isEmpty() || allowedValues.contains(value))
    }

    /**
     * A list of allowed values with descriptions. If empty than any value is
     * allowed.
     *
     * @return
     */
    public var allowedValues: List<Value> by config.item().convert(
        reader = {
            val value = it.value
            when {
                value?.list != null -> value.list
                type?.let { type -> type.size == 1 && type[0] === ValueType.BOOLEAN } ?: false -> listOf(True, False)
                else -> emptyList()
            }
        },
        writer = {
            MetaItemValue(it.asValue())
        }
    )

    /**
     * Allow given list of value and forbid others
     */
    public fun allow(vararg v: Any) {
        this.allowedValues = v.map { Value.of(it) }
    }

    override fun copy(): ValueDescriptor = ValueDescriptor(config.toConfig())
}

/**
 * Merge two node descriptors into one using first one as primary
 */
public operator fun NodeDescriptor.plus(other: NodeDescriptor): NodeDescriptor {
    return NodeDescriptor().apply {
        config.update(other.config)
        config.update(this@plus.config)
    }
}