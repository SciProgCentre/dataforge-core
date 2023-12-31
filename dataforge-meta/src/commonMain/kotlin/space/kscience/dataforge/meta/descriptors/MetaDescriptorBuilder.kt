package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutFirst
import space.kscience.dataforge.names.first
import space.kscience.dataforge.names.length
import kotlin.collections.set

public class MetaDescriptorBuilder @PublishedApi internal constructor() {
    public var description: String? = null

    @Deprecated("Replace by description", ReplaceWith("description"))
    public var info: String? by ::description

    public var children: MutableMap<String, MetaDescriptorBuilder> = linkedMapOf()
    public var multiple: Boolean = false
    public var valueRestriction: ValueRestriction = ValueRestriction.NONE

    public var valueTypes: List<ValueType>? = null

    public fun valueType(primaryType: ValueType, vararg otherTypes: ValueType) {
        valueTypes = listOf(primaryType, *otherTypes)
    }

    /**
     * A key for indexing values. Should be changed in case of the name clash.
     */
    public var indexKey: String = Meta.INDEX_KEY

    /**
     * The default value
     */
    public var default: Value? = null

    public fun default(value: Any?) {
        default = Value.of(value)
    }

    public var attributes: MutableMeta = MutableMeta()

    public inline fun attributes(block: MutableMeta.() -> Unit) {
        attributes.apply(block)
    }

    internal fun node(
        name: Name,
        descriptorBuilder: MetaDescriptorBuilder,
    ): Unit {
        when (name.length) {
            0 -> error("Can't set descriptor to root")
            1 -> {
                children[name.first().body] = descriptorBuilder
            }

            else -> children.getOrPut(name.first().body) {
                MetaDescriptorBuilder()
            }.node(name.cutFirst(), descriptorBuilder)
        }
    }

    internal fun node(
        name: Name,
        descriptorBuilder: MetaDescriptor,
    ): Unit {
        node(name, descriptorBuilder.toBuilder())
    }

    public var allowedValues: List<Value>
        get() = attributes[MetaDescriptor.ALLOWED_VALUES_KEY]?.value?.list ?: emptyList()
        set(value) {
            attributes[MetaDescriptor.ALLOWED_VALUES_KEY] = value
        }


    public fun allowedValues(vararg values: Any) {
        allowedValues = values.map { Value.of(it) }
    }

    public fun from(descriptor: MetaDescriptor) {
        description = descriptor.description
        children.putAll(descriptor.children.mapValues { it.value.toBuilder() })
        multiple = descriptor.multiple
        valueRestriction = descriptor.valueRestriction
        valueTypes = descriptor.valueTypes
        indexKey = descriptor.indexKey
        default = descriptor.defaultValue
        attributes.update(descriptor.attributes)
    }

    @PublishedApi
    internal fun build(): MetaDescriptor = MetaDescriptor(
        description = description,
        children = children.mapValues { it.value.build() },
        multiple = multiple,
        valueRestriction = valueRestriction,
        valueTypes = valueTypes,
        indexKey = indexKey,
        defaultValue = default,
        attributes = attributes
    )
}

//public fun MetaDescriptorBuilder.item(name: String, block: MetaDescriptorBuilder.() -> Unit): MetaDescriptorBuilder =
//    item(Name.parse(name), block)

public inline fun MetaDescriptor(block: MetaDescriptorBuilder.() -> Unit): MetaDescriptor =
    MetaDescriptorBuilder().apply(block).build()

/**
 * Create and configure child node descriptor
 */
public fun MetaDescriptorBuilder.node(
    name: Name,
    block: MetaDescriptorBuilder.() -> Unit,
) {
    node(
        name,
        MetaDescriptorBuilder().apply(block)
    )
}

public fun MetaDescriptorBuilder.node(name: String, descriptor: MetaDescriptor) {
    node(Name.parse(name), descriptor)
}

public fun MetaDescriptorBuilder.node(name: String, block: MetaDescriptorBuilder.() -> Unit) {
    node(Name.parse(name), block)
}

public fun MetaDescriptorBuilder.node(
    key: String,
    base: Described,
    block: MetaDescriptorBuilder.() -> Unit = {},
) {
    node(Name.parse(key), base.descriptor?.toBuilder()?.apply(block) ?: MetaDescriptorBuilder())
}

public fun MetaDescriptorBuilder.required() {
    valueRestriction = ValueRestriction.REQUIRED
}

private fun MetaDescriptor.toBuilder(): MetaDescriptorBuilder = MetaDescriptorBuilder().apply {
    description = this@toBuilder.description
    children = this@toBuilder.children.mapValuesTo(LinkedHashMap()) { it.value.toBuilder() }
    multiple = this@toBuilder.multiple
    valueRestriction = this@toBuilder.valueRestriction
    valueTypes = this@toBuilder.valueTypes
    indexKey = this@toBuilder.indexKey
    default = defaultValue
    attributes = this@toBuilder.attributes.toMutableMeta()
}


/**
 * Create and configure child value descriptor
 */
public fun MetaDescriptorBuilder.value(
    name: Name,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit = {},
): Unit = node(name) {
    valueType(type, *additionalTypes)
    block()
}

public fun MetaDescriptorBuilder.value(
    name: String,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit = {},
): Unit = value(Name.parse(name), type, additionalTypes = additionalTypes, block)


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

/**
 * Make a deep copy of this descriptor applying given transformation [block]
 */
public fun MetaDescriptor.copy(block: MetaDescriptorBuilder.() -> Unit = {}): MetaDescriptor =
    toBuilder().apply(block).build()