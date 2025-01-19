package space.kscience.dataforge.meta

import kotlinx.serialization.json.Json
import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaDescriptorBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.startsWith
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty


/**
 * A reference to a read-only value of type [T] inside [MetaProvider] or writable value in [MutableMetaProvider]
 */
@DFExperimental
public data class MetaRef<T>(
    public val name: Name,
    public val converter: MetaConverter<T>,
    override val descriptor: MetaDescriptor? = converter.descriptor,
) : Described

/**
 * Get a value from provider by [ref] or return null if node with given name is missing
 */
@DFExperimental
public operator fun <T> MetaProvider.get(ref: MetaRef<T>): T? = get(ref.name)?.let { ref.converter.readOrNull(it) }

/**
 * Set a value in a mutable provider by [ref]
 */
@DFExperimental
public operator fun <T> MutableMetaProvider.set(ref: MetaRef<T>, value: T) {
    set(ref.name, ref.converter.convert(value))
}

/**
 * Observe changes to specific property via given [ref].
 *
 * This listener should be removed in a same way as [ObservableMeta.onChange].
 *
 * @param callback an action to be performed on each change of item. Null means that the item is not present or malformed.
 */
@DFExperimental
public fun <T: Any> ObservableMeta.onValueChange(owner: Any?, ref: MetaRef<T>, callback: (T?) -> Unit) {
    onChange(owner) { name ->
        if (name.startsWith(ref.name)) {
            get(name)?.let { value ->
                callback(ref.converter.readOrNull(value))
            }
        }
    }
}

/**
 * Remove a node corresponding to [ref] from a mutable provider if it exists
 */
@DFExperimental
public fun MutableMetaProvider.remove(ref: MetaRef<*>) {
    remove(ref.name)
}

/**
 * Base storage of [MetaRef]
 */
@OptIn(DFExperimental::class)
public interface MetaRefStore : Described {
    public val refs: List<MetaRef<*>>
}

/**
 * A base class for [Meta] specification that stores references to meta nodes.
 */
@DFExperimental
public abstract class MetaSpec : MetaRefStore {
    private val _refs: MutableList<MetaRef<*>> = mutableListOf()
    override val refs: List<MetaRef<*>> get() = _refs

    /**
     * Register a ref in this specification
     */
    protected fun registerRef(ref: MetaRef<*>) {
        _refs.add(ref)
    }

    /**
     * Create and register a ref by property name and provided converter.
     * By default, uses descriptor from the converter
     */
    public fun <T> item(
        converter: MetaConverter<T>,
        key: Name? = null,
        descriptor: MetaDescriptor? = converter.descriptor,
    ): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<T>>> =
        PropertyDelegateProvider { _, property ->
            val ref = MetaRef(key ?: property.name.asName(), converter, descriptor)
            registerRef(ref)
            ReadOnlyProperty { _, _ ->
                ref
            }
        }

    /**
     * Override to provide custom [MetaDescriptor]
     */
    protected open fun MetaDescriptorBuilder.buildDescriptor(): Unit = Unit

    override val descriptor: MetaDescriptor by lazy {
        MetaDescriptor {
            refs.forEach { ref ->
                ref.descriptor?.let {
                    node(ref.name, ref.descriptor)
                }
            }
            buildDescriptor()
        }
    }
}

/**
 * Register an item using a [descriptorBuilder] to customize descriptor
 */
@DFExperimental
public fun <T> MetaSpec.item(
    converter: MetaConverter<T>,
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<T>>> = item(converter, key, MetaDescriptor {
    converter.descriptor?.let { from(it) }
    descriptorBuilder()
})

//utility methods to add different nodes

@DFExperimental
public fun MetaSpec.metaItem(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<Meta>>> =
    item(MetaConverter.meta, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.string(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<String>>> =
    item(MetaConverter.string, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.boolean(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<Boolean>>> =
    item(MetaConverter.boolean, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.stringList(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<List<String>>>> =
    item(MetaConverter.stringList, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.float(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<Float>>> =
    item(MetaConverter.float, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.double(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<Double>>> =
    item(MetaConverter.double, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.int(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<Int>>> =
    item(MetaConverter.int, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.long(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<Long>>> =
    item(MetaConverter.long, key, descriptorBuilder)


@DFExperimental
public fun MetaSpec.doubleArray(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<DoubleArray>>> =
    item(MetaConverter.doubleArray, key, descriptorBuilder)

@DFExperimental
public fun MetaSpec.byteArray(
    key: Name? = null,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<ByteArray>>> =
    item(MetaConverter.byteArray, key, descriptorBuilder)

@DFExperimental
public inline fun <reified E : Enum<E>> MetaSpec.enum(
    key: Name? = null,
    noinline descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<E>>> =
    item(MetaConverter.enum(), key, descriptorBuilder)

@DFExperimental
public inline fun <reified T> MetaSpec.serializable(
    key: Name? = null,
    jsonEncoder: Json = Json,
    noinline descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<T>>> =
    item(MetaConverter.serializable(jsonEncoder = jsonEncoder), key, descriptorBuilder)