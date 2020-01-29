package hep.dataforge.meta.descriptors

import hep.dataforge.meta.*
import hep.dataforge.meta.scheme.*
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.asName
import hep.dataforge.names.isEmpty
import hep.dataforge.values.False
import hep.dataforge.values.True
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType

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
class NodeDescriptor : ItemDescriptor() {

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

    /**
     * The map of children node descriptors
     */
    val nodes: Map<String, NodeDescriptor>
        get() = config.getIndexed(NODE_KEY.asName()).entries.associate { (name, node) ->
            name to wrap(node.node ?: error("Node descriptor must be a node"))
        }

    /**
     * Define a child item descriptor for this node
     */
    fun defineItem(name: String, descriptor: ItemDescriptor) {
        if (items.keys.contains(name)) error("The key $name already exists in descriptor")
        val token = when (descriptor) {
            is NodeDescriptor -> NameToken(NODE_KEY, name)
            is ValueDescriptor -> NameToken(VALUE_KEY, name)
        }
        config[token] = descriptor.config

    }


    fun defineNode(name: String, block: NodeDescriptor.() -> Unit) {
        val token = NameToken(NODE_KEY, name)
        if (config[token] == null) {
            config[token] = NodeDescriptor(block)
        } else {
            NodeDescriptor.update(config[token].node ?: error("Node expected"), block)
        }
    }

    private fun buildNode(name: Name): NodeDescriptor {
        return when (name.length) {
            0 -> this
            1 -> {
                val token = NameToken(NODE_KEY, name.toString())
                val config: Config = config[token].node ?: Config().also { config[token] = it }
                wrap(config)
            }
            else -> buildNode(name.first()?.asName()!!).buildNode(name.cutFirst())
        }
    }

    fun defineNode(name: Name, block: NodeDescriptor.() -> Unit) {
        buildNode(name).apply(block)
    }

    /**
     * The list of value descriptors
     */
    val values: Map<String, ValueDescriptor>
        get() = config.getIndexed(VALUE_KEY.asName()).entries.associate { (name, node) ->
            name to ValueDescriptor.wrap(node.node ?: error("Value descriptor must be a node"))
        }


    /**
     * Add a value descriptor using block for
     */
    fun defineValue(name: String, block: ValueDescriptor.() -> Unit) {
        defineItem(name, ValueDescriptor(block))
    }

    fun defineValue(name: Name, block: ValueDescriptor.() -> Unit) {
        require(name.length >= 1) { "Name length for value descriptor must be non-empty" }
        buildNode(name.cutLast()).defineValue(name.last().toString(), block)
    }

    val items: Map<String, ItemDescriptor> get() = nodes + values


//override val descriptor: NodeDescriptor =  empty("descriptor")

    companion object : SchemeSpec<NodeDescriptor>(::NodeDescriptor) {

        //        const val ITEM_KEY = "item"
        const val NODE_KEY = "node"
        const val VALUE_KEY = "value"

        //override fun wrap(config: Config): NodeDescriptor = NodeDescriptor(config)

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

    companion object : SchemeSpec<ValueDescriptor>(::ValueDescriptor) {
        inline fun <reified E : Enum<E>> enum(name: String) = ValueDescriptor {
            type(ValueType.STRING)
            this.allowedValues = enumValues<E>().map { Value.of(it.name) }
        }

//        /**
//         * Build a value descriptor from annotation
//         */
//        fun build(def: ValueDef): ValueDescriptor {
//            val builder = MetaBuilder("value")
//                .setValue("name", def.key)
//
//            if (def.type.isNotEmpty()) {
//                builder.setValue("type", def.type)
//            }
//
//            if (def.multiple) {
//                builder.setValue("multiple", def.multiple)
//            }
//
//            if (!def.info.isEmpty()) {
//                builder.setValue("info", def.info)
//            }
//
//            if (def.allowed.isNotEmpty()) {
//                builder.setValue("allowedValues", def.allowed)
//            } else if (def.enumeration != Any::class) {
//                if (def.enumeration.java.isEnum) {
//                    val values = def.enumeration.java.enumConstants
//                    builder.setValue("allowedValues", values.map { it.toString() })
//                } else {
//                    throw RuntimeException("Only enumeration classes are allowed in 'enumeration' annotation property")
//                }
//            }
//
//            if (def.def.isNotEmpty()) {
//                builder.setValue("default", def.def)
//            } else if (!def.required) {
//                builder.setValue("required", def.required)
//            }
//
//            if (def.tags.isNotEmpty()) {
//                builder.setValue("tags", def.tags)
//            }
//            return ValueDescriptor(builder)
//        }
//
//        /**
//         * Build empty value descriptor
//         */
//        fun empty(valueName: String): ValueDescriptor {
//            val builder = MetaBuilder("value")
//                .setValue("name", valueName)
//            return ValueDescriptor(builder)
//        }
//
//        /**
//         * Merge two separate value descriptors
//         */
//        fun merge(primary: ValueDescriptor, secondary: ValueDescriptor): ValueDescriptor {
//            return ValueDescriptor(Laminate(primary.meta, secondary.meta))
//        }
    }
}