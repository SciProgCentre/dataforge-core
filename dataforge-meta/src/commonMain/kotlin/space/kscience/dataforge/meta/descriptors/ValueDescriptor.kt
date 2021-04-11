package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.values.*



/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
@DFBuilder
public sealed interface ValueDescriptor: ItemDescriptor{

    /**
     * True if the value is required
     *
     * @return
     */
    override val required: Boolean

    /**
     * The default for this value. Null if there is no default.
     *
     * @return
     */
    public val default: Value?


    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    public val type: List<ValueType>?
    /**
     * Check if given value is allowed for here. The type should be allowed and
     * if it is value should be within allowed values
     *
     * @param value
     * @return
     */
    public fun isAllowedValue(value: Value): Boolean =
        (type?.let { it.contains(ValueType.STRING) || it.contains(value.type) } ?: true)
                && (allowedValues.isEmpty() || allowedValues.contains(value))

    /**
     * A list of allowed values with descriptions. If empty than any value is
     * allowed.
     *
     * @return
     */
    public val allowedValues: List<Value>
}

/**
 * A builder fir [ValueDescriptor]
 */
@DFBuilder
public class ValueDescriptorBuilder(config: Config = Config()) : ItemDescriptorBuilder(config), ValueDescriptor {

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
    override var default: Value? by config.value()

    public fun default(v: Any) {
        this.default = Value.of(v)
    }

    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    override var type: List<ValueType>? by config.listValue { ValueType.valueOf(it.string) }

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
    override fun isAllowedValue(value: Value): Boolean {
        return (type?.let { it.contains(ValueType.STRING) || it.contains(value.type) } ?: true)
                && (allowedValues.isEmpty() || allowedValues.contains(value))
    }

    /**
     * A list of allowed values with descriptions. If empty than any value is
     * allowed.
     *
     * @return
     */
    override var allowedValues: List<Value> by config.item().convert(
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

    override fun build(): ValueDescriptor = ValueDescriptorBuilder(config.copy())
}
