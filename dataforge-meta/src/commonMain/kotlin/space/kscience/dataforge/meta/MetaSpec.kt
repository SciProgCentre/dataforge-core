package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaDescriptorBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
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
 * Remove a node corresponding to [ref] from a mutable provider if it exists
 */
@DFExperimental
public fun MutableMetaProvider.remove(ref: MetaRef<*>) {
    remove(ref.name)
}

/**
 * A base class for [Meta] specification that stores references to meta nodes
 */
@DFExperimental
public abstract class MetaSpec : Described {
    private val refs: MutableList<MetaRef<*>> = mutableListOf()

    protected fun registerRef(ref: MetaRef<*>) {
        refs.add(ref)
    }

    public fun <T> item(
        converter: MetaConverter<T>,
        descriptor: MetaDescriptor? = converter.descriptor,
        key: Name? = null,
    ): PropertyDelegateProvider<MetaSpec, ReadOnlyProperty<MetaSpec, MetaRef<T>>> =
        PropertyDelegateProvider { _, property ->
            val ref = MetaRef(key ?: property.name.asName(), converter, descriptor)
            registerRef(ref)
            ReadOnlyProperty { _, _ ->
                ref
            }
        }

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