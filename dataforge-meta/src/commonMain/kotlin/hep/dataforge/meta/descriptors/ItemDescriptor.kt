package hep.dataforge.meta.descriptors

import hep.dataforge.meta.*
import hep.dataforge.names.*
import hep.dataforge.values.False
import hep.dataforge.values.True
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType

@DFBuilder
sealed class ItemDescriptor : Scheme() {

    /**
     * True if same name siblings with this name are allowed
     *
     * @return
     */
    var multiple: Boolean by boolean(false)

    /**
     * The item description
     *
     * @return
     */
    var info: String? by string()

    /**
     * Additional attributes of an item. For example validation and widget parameters
     *
     * @return
     */
    var attributes by config()

    /**
     * True if the item is required
     *
     * @return
     */
    abstract var required: Boolean
}

/**
 * Configure attributes of the descriptor
 */
fun ItemDescriptor.attributes(block: Config.() -> Unit) {
    (attributes ?: Config().also { this.config = it }).apply(block)
}

/**
 * Check if given item suits the descriptor
 */
fun ItemDescriptor.validateItem(item: MetaItem<*>?): Boolean {
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
class NodeDescriptor private constructor() : ItemDescriptor() {
    /**
     * True if the node is required
     *
     * @return
     */
    override var required: Boolean by boolean { default == null }

    /**
     * The default for this node. Null if there is no default.
     *
     * @return
     */
    var default by node()

    val items: Map<String, ItemDescriptor>
        get() = config.getIndexed(ITEM_KEY).mapValues { (_, item) ->
            val node = item.node ?: error("Node descriptor must be a node")
            if (node[IS_NODE_KEY].boolean == true) {
                NodeDescriptor.wrap(node)
            } else {
                ValueDescriptor.wrap(node)
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
            name to NodeDescriptor.wrap(node)
        }

    /**
     * The list of value descriptors
     */
    val values: Map<String, ValueDescriptor>
        get() = config.getIndexed(ITEM_KEY).entries.filter {
            it.value.node[IS_NODE_KEY].boolean != true
        }.associate { (name, item) ->
            val node = item.node ?: error("Node descriptor must be a node")
            name to ValueDescriptor.wrap(node)
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
                wrap(config)
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

    fun defineItem(name: Name, descriptor: ItemDescriptor) {
        buildNode(name.cutLast()).newItem(name.last().toString(), descriptor)
    }

    fun defineItem(name: String, descriptor: ItemDescriptor) {
        defineItem(name.toName(), descriptor)
    }

    fun defineNode(name: Name, block: NodeDescriptor.() -> Unit) {
        defineItem(name, NodeDescriptor(block))
    }

    fun defineNode(name: String, block: NodeDescriptor.() -> Unit) {
        defineNode(name.toName(), block)
    }

    fun defineValue(name: Name, block: ValueDescriptor.() -> Unit) {
        require(name.length >= 1) { "Name length for value descriptor must be non-empty" }
        defineItem(name, ValueDescriptor(block))
    }

    fun defineValue(name: String, block: ValueDescriptor.() -> Unit) {
        defineValue(name.toName(), block)
    }

    companion object : SchemeSpec<NodeDescriptor>(::NodeDescriptor) {

        val ITEM_KEY = "item".asName()
        val IS_NODE_KEY = "@isNode".asName()

        override fun empty(): NodeDescriptor {
            return super.empty().apply {
                config[IS_NODE_KEY] = true
            }
        }

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

/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
@DFBuilder
class ValueDescriptor : ItemDescriptor() {

    /**
     * True if the value is required
     *
     * @return
     */
    override var required: Boolean by boolean { default == null }

    /**
     * The default for this value. Null if there is no default.
     *
     * @return
     */
    var default: Value? by value()

    fun default(v: Any) {
        this.default = Value.of(v)
    }

    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    var type: List<ValueType> by item {
        it?.value?.list?.map { v -> ValueType.valueOf(v.string) } ?: emptyList()
    }

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
        return (type.isEmpty() || type.contains(ValueType.STRING) || type.contains(value.type)) && (allowedValues.isEmpty() || allowedValues.contains(
            value
        ))
    }

    /**
     * A list of allowed values with descriptions. If empty than any value is
     * allowed.
     *
     * @return
     */
    var allowedValues: List<Value> by value {
        it?.list ?: if (type.size == 1 && type[0] === ValueType.BOOLEAN) {
            listOf(True, False)
        } else {
            emptyList()
        }
    }

    /**
     * Allow given list of value and forbid others
     */
    fun allow(vararg v: Any) {
        this.allowedValues = v.map { Value.of(it) }
    }

    companion object : SchemeSpec<ValueDescriptor>(::ValueDescriptor)
}