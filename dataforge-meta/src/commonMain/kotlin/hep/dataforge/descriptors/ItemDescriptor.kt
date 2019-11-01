package hep.dataforge.descriptors

import hep.dataforge.meta.*
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName
import hep.dataforge.values.False
import hep.dataforge.values.True
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType

sealed class ItemDescriptor(override val config: Config) : Specific {

    /**
     * The name of this item
     *
     * @return
     */
    var name: String by string { error("Anonymous descriptors are not allowed") }

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
    var attributes by node()

    /**
     * True if the item is required
     *
     * @return
     */
    abstract var required: Boolean
}

/**
 * Descriptor for meta node. Could contain additional information for viewing
 * and editing.
 *
 * @author Alexander Nozik
 */
class NodeDescriptor(config: Config) : ItemDescriptor(config) {

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
    var default: Config? by node()

    /**
     * The list of value descriptors
     */
    val values: Map<String, ValueDescriptor>
        get() = config.getIndexed(VALUE_KEY.toName()).entries.associate { (name, node) ->
            name to ValueDescriptor.wrap(node.node ?: error("Value descriptor must be a node"))
        }

    fun value(name: String, descriptor: ValueDescriptor) {
        if (items.keys.contains(name)) error("The key $name already exists in descriptor")
        val token = NameToken(VALUE_KEY, name)
        config[token] = descriptor.config
    }

    /**
     * Add a value descriptor using block for
     */
    fun value(name: String, block: ValueDescriptor.() -> Unit) {
        value(name, ValueDescriptor.build { this.name = name }.apply(block))
    }

    /**
     * The map of children node descriptors
     */
    val nodes: Map<String, NodeDescriptor>
        get() = config.getIndexed(NODE_KEY.toName()).entries.associate { (name, node) ->
            name to wrap(node.node ?: error("Node descriptor must be a node"))
        }


    fun node(name: String, descriptor: NodeDescriptor) {
        if (items.keys.contains(name)) error("The key $name already exists in descriptor")
        val token = NameToken(NODE_KEY, name)
        config[token] = descriptor.config
    }

    fun node(name: String, block: NodeDescriptor.() -> Unit) {
        node(name, build { this.name = name }.apply(block))
    }

    val items: Map<String, ItemDescriptor> get() = nodes + values


    //override val descriptor: NodeDescriptor =  empty("descriptor")

    companion object : Specification<NodeDescriptor> {

        //        const val ITEM_KEY = "item"
        const val NODE_KEY = "node"
        const val VALUE_KEY = "value"

        override fun wrap(config: Config): NodeDescriptor = NodeDescriptor(config)

        //TODO infer descriptor from spec
    }
}


/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
class ValueDescriptor(config: Config) : ItemDescriptor(config) {


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
    var type: List<ValueType> by value {
        it?.list?.map { v -> ValueType.valueOf(v.string) } ?: emptyList()
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

    companion object : Specification<ValueDescriptor> {

        override fun wrap(config: Config): ValueDescriptor = ValueDescriptor(config)

        inline fun <reified E : Enum<E>> enum(name: String) =
            build {
                this.name = name
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
