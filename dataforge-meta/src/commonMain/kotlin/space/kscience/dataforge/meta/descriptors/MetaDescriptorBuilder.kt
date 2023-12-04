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

    public fun item(name: Name, block: MetaDescriptorBuilder.() -> Unit = {}): MetaDescriptorBuilder {
        return when (name.length) {
            0 -> apply(block)
            1 -> {
                val target = MetaDescriptorBuilder().apply(block)
                children[name.first().body] = target
                target
            }

            else -> {
                children.getOrPut(name.first().body) { MetaDescriptorBuilder() }.item(name.cutFirst(), block)
            }
        }
    }

    public fun node(
        name: Name,
        descriptor: MetaDescriptor,
        block: MetaDescriptorBuilder.() -> Unit = {},
    ): MetaDescriptorBuilder = when (name.length) {
        0 -> error("Can't set descriptor to root")
        1 -> {
            val item = descriptor.toBuilder().apply {
                valueRestriction = ValueRestriction.ABSENT
            }.apply(block)
            children[name.first().body] = item
            item
        }

        else -> children.getOrPut(name.first().body) {
            MetaDescriptorBuilder()
        }.node(name.cutFirst(), descriptor, block)
    }

    public var allowedValues: List<Value>
        get() = attributes[MetaDescriptor.ALLOWED_VALUES_KEY]?.value?.list ?: emptyList()
        set(value) {
            attributes[MetaDescriptor.ALLOWED_VALUES_KEY] = value
        }


    public fun allowedValues(vararg values: Any) {
        allowedValues = values.map { Value.of(it) }
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

public fun MetaDescriptorBuilder.item(name: String, block: MetaDescriptorBuilder.() -> Unit): MetaDescriptorBuilder =
    item(Name.parse(name), block)

public inline fun MetaDescriptor(block: MetaDescriptorBuilder.() -> Unit): MetaDescriptor =
    MetaDescriptorBuilder().apply(block).build()

/**
 * Create and configure child value descriptor
 */
public fun MetaDescriptorBuilder.value(
    name: Name,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit = {},
): MetaDescriptorBuilder = item(name) {
    valueType(type, *additionalTypes)
    block()
}

public fun MetaDescriptorBuilder.value(
    name: String,
    type: ValueType,
    vararg additionalTypes: ValueType,
    block: MetaDescriptorBuilder.() -> Unit = {},
): MetaDescriptorBuilder = value(Name.parse(name), type, additionalTypes = additionalTypes, block)

/**
 * Create and configure child value descriptor
 */
public fun MetaDescriptorBuilder.node(
    name: Name, block: MetaDescriptorBuilder.() -> Unit,
): MetaDescriptorBuilder = item(name) {
    valueRestriction = ValueRestriction.ABSENT
    block()
}

public fun MetaDescriptorBuilder.node(name: String, block: MetaDescriptorBuilder.() -> Unit) {
    node(Name.parse(name), block)
}

public fun MetaDescriptorBuilder.node(
    key: String,
    described: Described,
    block: MetaDescriptorBuilder.() -> Unit = {},
) {
    described.descriptor?.let {
        node(Name.parse(key), it, block)
    }
}

public fun MetaDescriptorBuilder.required() {
    valueRestriction = ValueRestriction.REQUIRED
}

public inline fun <reified E : Enum<E>> MetaDescriptorBuilder.enum(
    key: Name,
    default: E?,
    crossinline modifier: MetaDescriptorBuilder.() -> Unit = {},
): MetaDescriptorBuilder = value(key, ValueType.STRING) {
    default?.let {
        this.default = default.asValue()
    }
    allowedValues = enumValues<E>().map { it.asValue() }
    modifier()
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
 * Make a deep copy of this descriptor applying given transformation [block]
 */
public fun MetaDescriptor.copy(block: MetaDescriptorBuilder.() -> Unit = {}): MetaDescriptor =
    toBuilder().apply(block).build()