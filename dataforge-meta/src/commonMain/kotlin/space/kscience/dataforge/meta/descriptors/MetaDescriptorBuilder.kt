package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.asValue

public class MetaDescriptorBuilder {
    public var info: String? = null
    public var children: MutableMap<String, MetaDescriptorBuilder> = hashMapOf()
    public var multiple: Boolean = false
    public var required: Boolean = false

    public var type: List<ValueType>? = null

    public fun type(primaryType: ValueType, vararg otherTypes: ValueType) {
        type = listOf(primaryType, *otherTypes)
    }

    public var indexKey: String = Meta.INDEX_KEY
    public var default: Value? = null

    public fun default(value: Any?) {
        default = Value.of(value)
    }

    public var attributes: MutableMeta = MutableMeta()

    public fun item(name: Name, block: MetaDescriptorBuilder.() -> Unit) {
        when (name.length) {
            0 -> apply(block)
            1 -> {
                val target = MetaDescriptorBuilder().apply(block)
                children[name.first().body] = target
            }
            else -> {
                children.getOrPut(name.first().body) { MetaDescriptorBuilder() }.item(name.cutFirst(), block)
            }
        }
    }

    public var allowedValues: List<Value>
        get() = attributes[MetaDescriptor.ALLOWED_VALUES_KEY]?.value?.list ?: emptyList()
        set(value) {
            attributes[MetaDescriptor.ALLOWED_VALUES_KEY] = value
        }


    public fun allowedValues(vararg values: Any) {
        allowedValues = values.map { Value.of(it) }
    }

    internal fun build(): MetaDescriptor = MetaDescriptor(
        info = info,
        children = children.mapValues { it.value.build() },
        multiple = multiple,
        required = required,
        type = type,
        indexKey = indexKey,
        defaultValue = default,
        attributes = attributes
    )
}

public fun MetaDescriptorBuilder.item(name: String, block: MetaDescriptorBuilder.() -> Unit) {
    item(name.toName(), block)
}

public fun MetaDescriptor(block: MetaDescriptorBuilder.() -> Unit): MetaDescriptor =
    MetaDescriptorBuilder().apply(block).build()

/**
 * Create and configure child value descriptor
 */
public fun MetaDescriptorBuilder.value(
    name: Name,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit
) {
    item(name) {
        type(type, *additionalTypes)
        block()
    }
}

public fun MetaDescriptorBuilder.value(
    name: String,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit
) {
    value(name.toName(), type, additionalTypes = additionalTypes, block)
}

/**
 * Create and configure child value descriptor
 */
public fun MetaDescriptorBuilder.node(name: Name, block: MetaDescriptorBuilder.() -> Unit) {
    item(name) {
        type(ValueType.NULL)
        block()
    }
}

public fun MetaDescriptorBuilder.node(name: String, block: MetaDescriptorBuilder.() -> Unit) {
    node(name.toName(), block)
}

public inline fun <reified E : Enum<E>> MetaDescriptorBuilder.enum(
    key: Name,
    default: E?,
    crossinline modifier: MetaDescriptorBuilder.() -> Unit = {},
): Unit = value(key,ValueType.STRING) {
    default?.let {
        this.default = default.asValue()
    }
    allowedValues = enumValues<E>().map { it.asValue() }
    modifier()
}