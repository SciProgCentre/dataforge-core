package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutFirst
import space.kscience.dataforge.names.first
import space.kscience.dataforge.names.length
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.asValue
import kotlin.collections.set

public class MetaDescriptorBuilder internal constructor() {
    public var info: String? = null
    public var children: MutableMap<String, MetaDescriptorBuilder> = hashMapOf()
    public var multiple: Boolean = false
    public var valueRequirement: ValueRequirement = ValueRequirement.NONE

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

    public inline fun attributes(block: MutableMeta.() -> Unit) {
        attributes.apply(block)
    }

    public fun item(name: Name, descriptor: MetaDescriptor, block: MetaDescriptorBuilder.() -> Unit) {
        when (name.length) {
            0 -> {
            }
            1 -> children[name.first().body] = descriptor.toBuilder().apply(block)
            else -> children.getOrPut(name.first().body) {
                MetaDescriptorBuilder()
            }.item(name.cutFirst(), descriptor, block)
        }
    }

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
        valueRequirement = valueRequirement,
        valueTypes = type,
        indexKey = indexKey,
        defaultValue = default,
        attributes = attributes
    )
}

public fun MetaDescriptorBuilder.item(name: String, block: MetaDescriptorBuilder.() -> Unit) {
    item(Name.parse(name), block)
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
): Unit = item(name) {
    type(type, *additionalTypes)
    block()
}

public fun MetaDescriptorBuilder.value(
    name: String,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit
): Unit = value(Name.parse(name), type, additionalTypes = additionalTypes, block)

/**
 * Create and configure child value descriptor
 */
public fun MetaDescriptorBuilder.node(name: Name, block: MetaDescriptorBuilder.() -> Unit): Unit = item(name) {
    valueRequirement = ValueRequirement.ABSENT
    block()
}

public fun MetaDescriptorBuilder.node(name: String, block: MetaDescriptorBuilder.() -> Unit) {
    node(Name.parse(name), block)
}

public fun MetaDescriptorBuilder.required() {
    valueRequirement = ValueRequirement.REQUIRED
}

public inline fun <reified E : Enum<E>> MetaDescriptorBuilder.enum(
    key: Name,
    default: E?,
    crossinline modifier: MetaDescriptorBuilder.() -> Unit = {},
): Unit = value(key, ValueType.STRING) {
    default?.let {
        this.default = default.asValue()
    }
    allowedValues = enumValues<E>().map { it.asValue() }
    modifier()
}

private fun MetaDescriptor.toBuilder(): MetaDescriptorBuilder = MetaDescriptorBuilder().apply {
    info = this@toBuilder.info
    children = this@toBuilder.children.mapValuesTo(LinkedHashMap()) { it.value.toBuilder() }
    multiple = this@toBuilder.multiple
    valueRequirement = this@toBuilder.valueRequirement
    type = this@toBuilder.valueTypes
    indexKey = this@toBuilder.indexKey
    default = defaultValue
    attributes = this@toBuilder.attributes.toMutableMeta()
}

/**
 * Make a deep copy of this descriptor applying given transformation [block]
 */
public fun MetaDescriptor.copy(block: MetaDescriptorBuilder.() -> Unit = {}): MetaDescriptor =
    toBuilder().apply(block).build()