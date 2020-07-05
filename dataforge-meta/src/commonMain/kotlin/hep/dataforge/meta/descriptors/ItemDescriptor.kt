package hep.dataforge.meta.descriptors

import hep.dataforge.meta.*
import hep.dataforge.names.*
import hep.dataforge.values.*

@DFBuilder
sealed class ItemDescriptor(val config: Config) {

    /**
     * True if same name siblings with this name are allowed
     *
     * @return
     */
    var multiple: Boolean by config.boolean(false)

    /**
     * The item description
     *
     * @return
     */
    var info: String? by config.string()

    /**
     * True if the item is required
     *
     * @return
     */
    abstract var required: Boolean


    /**
     * Additional attributes of an item. For example validation and widget parameters
     *
     * @return
     */
    var attributes by config.node()
}

/**
 * Configure attributes of the descriptor
 */
fun ItemDescriptor.attributes(block: Config.() -> Unit) {
    (attributes ?: Config().also { this.attributes = it }).apply(block)
}

/**
 * Set specific attribute in the descriptor
 */
fun ItemDescriptor.setAttribute(name: Name, value: Any?) {
    attributes {
        set(name, value)
    }
}

/**
 * Check if given item suits the descriptor
 */
fun ItemDescriptor.validateItem(item: MetaItem<*>?): Boolean {
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
class NodeDescriptor(config: Config = Config()) : ItemDescriptor(config) {
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
    var default by config.node()

    /**
     * An index field by which this node is identified in case of same name siblings construct
     */
    var indexKey by config.string(DEFAULT_INDEX_KEY)

    val items: Map<String, ItemDescriptor>
        get() = config.getIndexed(ITEM_KEY).mapValues { (_, item) ->
            val node = item.node ?: error("Node descriptor must be a node")
            if (node[IS_NODE_KEY].boolean == true) {
                NodeDescriptor(node)
            } else {
                ValueDescriptor(node)
            }
        }

    /**
     * The map of children node descriptors
     */
    @Suppress("UNCHECKED_CAST")
    val nodes: Map<String, NodeDescriptor>
        get() = config.getIndexed(ITEM_KEY).entries.filter {
            it.value.node[IS_NODE_KEY].boolean == true
        }.associate { (name, item) ->
            val node = item.node ?: error("Node descriptor must be a node")
            name to NodeDescriptor(node)
        }

    /**
     * The list of value descriptors
     */
    val values: Map<String, ValueDescriptor>
        get() = config.getIndexed(ITEM_KEY).entries.filter {
            it.value.node[IS_NODE_KEY].boolean != true
        }.associate { (name, item) ->
            val node = item.node ?: error("Node descriptor must be a node")
            name to ValueDescriptor(node)
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
            else -> buildNode(name.first()?.asName()!!).buildNode(name.cutFirst())
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

    fun item(name: Name, descriptor: ItemDescriptor) {
        buildNode(name.cutLast()).newItem(name.last().toString(), descriptor)
    }

    fun item(name: String, descriptor: ItemDescriptor) {
        item(name.toName(), descriptor)
    }

    fun node(name: Name, block: NodeDescriptor.() -> Unit) {
        item(name, NodeDescriptor().apply(block))
    }

    fun node(name: String, block: NodeDescriptor.() -> Unit) {
        node(name.toName(), block)
    }

    fun value(name: Name, block: ValueDescriptor.() -> Unit) {
        require(name.length >= 1) { "Name length for value descriptor must be non-empty" }
        item(name, ValueDescriptor().apply(block))
    }

    fun value(name: String, block: ValueDescriptor.() -> Unit) {
        value(name.toName(), block)
    }

    companion object {

        val ITEM_KEY = "item".asName()
        val IS_NODE_KEY = "@isNode".asName()

        const val DEFAULT_INDEX_KEY = "@index"

        inline operator fun invoke(block: NodeDescriptor.() -> Unit) = NodeDescriptor().apply(block)

        //TODO infer descriptor from spec
    }
}

/**
 * Get a descriptor item associated with given name or null if item for given name not provided
 */
operator fun ItemDescriptor.get(name: Name): ItemDescriptor? {
    if (name.isEmpty()) return this
    return when (this) {
        is ValueDescriptor -> null // empty name already checked
        is NodeDescriptor -> items[name.first()!!.toString()]?.get(name.cutFirst())
    }
}

operator fun ItemDescriptor.get(name: String): ItemDescriptor? = get(name.toName())

/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
@DFBuilder
class ValueDescriptor(config: Config = Config()) : ItemDescriptor(config) {

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
    var default: Value? by config.value()

    fun default(v: Any) {
        this.default = Value.of(v)
    }

    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    var type: List<ValueType>? by config.listValue { ValueType.valueOf(it.string) }

    fun type(vararg t: ValueType) {
        this.type = listOf(*t)
    }

    /**
     * Check if given value is allowed for here. The type should be allowed and
     * if it is value should be within allowed values
     *
     * @param value
     * @return
     */
    fun isAllowedValue(value: Value): Boolean {
        return (type?.let { it.contains(ValueType.STRING) || it.contains(value.type) } ?: true)
                && (allowedValues.isEmpty() || allowedValues.contains(value))
    }

    /**
     * A list of allowed values with descriptions. If empty than any value is
     * allowed.
     *
     * @return
     */
    var allowedValues: List<Value> by config.item().convert(
        reader = {
            val value = it.value
            when {
                value?.list != null -> value.list
                type?.let { type -> type.size == 1 && type[0] === ValueType.BOOLEAN} ?: false -> listOf(True, False)
                else -> emptyList()
            }
        },
        writer = {
            MetaItem.ValueItem(it.asValue())
        }
    )

    /**
     * Allow given list of value and forbid others
     */
    fun allow(vararg v: Any) {
        this.allowedValues = v.map { Value.of(it) }
    }
}

/**
 * Merge two node descriptors into one using first one as primary
 */
operator fun NodeDescriptor.plus(other: NodeDescriptor): NodeDescriptor {
    return NodeDescriptor().apply {
        config.update(other.config)
        config.update(this@plus.config)
    }
}