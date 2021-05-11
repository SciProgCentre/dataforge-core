package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

public interface ReadOnlySpecification<out T : ItemProvider> {

    /**
     * Read generic read-only meta with this [Specification] producing instance of desired type.
     */
    public fun read(items: ItemProvider): T


    /**
     * Generate an empty object
     */
    public fun empty(): T

    /**
     * A convenience method to use specifications in builders
     */
    public operator fun invoke(action: T.() -> Unit): T = empty().apply(action)
}


/**
 * Allows to apply custom configuration in a type safe way to simple untyped configuration.
 * By convention [Scheme] companion should inherit this class
 *
 */
public interface Specification<out T : MutableItemProvider> : ReadOnlySpecification<T> {
    /**
     * Wrap [MutableItemProvider], using it as inner storage (changes to [Specification] are reflected on [MutableItemProvider]
     */
    public fun write(target: MutableItemProvider, defaultProvider: ItemProvider = ItemProvider.EMPTY): T
}

/**
 * Update a [MutableItemProvider] using given specification
 */
public fun <T : MutableItemProvider> MutableItemProvider.update(spec: Specification<T>, action: T.() -> Unit) {
    spec.write(this).apply(action)
}

/**
 * Update configuration using given specification
 */
public fun <C : MutableItemProvider, S : Specification<C>> Configurable.update(
    spec: S,
    action: C.() -> Unit,
) {
    config.update(spec, action)
}

public fun <T : MutableItemProvider> TypedMetaItem<MutableMeta<*>>.withSpec(spec: Specification<T>): T? =
    node?.let { spec.write(it) }

/**
 * A delegate that uses a [Specification] to wrap a child of this provider
 */
public fun <T : Scheme> MutableItemProvider.spec(
    spec: Specification<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return getChild(name).let { spec.write(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        set(name, value.toMeta().asMetaItem())
    }
}

/**
 * A delegate that uses a [Specification] to wrap a list of child providers.
 * If children are mutable, the changes in list elements are reflected on them.
 * The list is a snapshot of children state, so change in structure is not reflected on its composition.
 */
@DFExperimental
public fun <T : Scheme> MutableItemProvider.listOfSpec(
    spec: Specification<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, List<T>> = object : ReadWriteProperty<Any?, List<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: property.name.asName()
        return getIndexed(name).map {
            when (val value = it.value) {
                is MetaItemNode<*> -> when (value.node) {
                    is MutableItemProvider -> spec.write(value.node)
                    else -> spec.read(value.node)
                }
                is MetaItemValue -> spec.read(value)
            }
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        val name = key ?: property.name.asName()
        setIndexed(name, value.map { it.toMeta() })
    }
}
