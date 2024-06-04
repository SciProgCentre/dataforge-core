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
 * A reference to a read-only value of type [T] inside [MetaProvider]
 */
@DFExperimental
public data class MetaRef<T>(
    public val name: Name,
    public val converter: MetaConverter<T>,
    override val descriptor: MetaDescriptor? = converter.descriptor,
) : Described

@DFExperimental
public operator fun <T> MetaProvider.get(ref: MetaRef<T>): T? = get(ref.name)?.let { ref.converter.readOrNull(it) }

@DFExperimental
public operator fun <T> MutableMetaProvider.set(ref: MetaRef<T>, value: T) {
    set(ref.name, ref.converter.convert(value))
}

@DFExperimental
public class MetaSpec(
    private val configuration: MetaDescriptorBuilder.() -> Unit = {},
) : Described {
    private val refs: MutableList<MetaRef<*>> = mutableListOf()

    private fun registerRef(ref: MetaRef<*>) {
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

    override val descriptor: MetaDescriptor by lazy {
        MetaDescriptor {
            refs.forEach { ref ->
                ref.descriptor?.let {
                    node(ref.name, ref.descriptor)
                }
            }
            configuration()
        }
    }
}