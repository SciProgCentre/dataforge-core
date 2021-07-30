package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

public interface ReadOnlySpecification<out T : Any> {

    /**
     * Read generic read-only meta with this [Specification] producing instance of desired type.
     * The source is not mutated even if it is in theory mutable
     */
    public fun read(source: Meta): T

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
public interface Specification<out T : Any> : ReadOnlySpecification<T> {
    /**
     * Wrap [MutableMeta], using it as inner storage (changes to [Specification] are reflected on [MutableMeta]
     */
    public fun write(target: MutableMeta): T
}

/**
 * Update a [MutableMeta] using given specification
 */
public fun <T : Any> MutableMeta.update(
    spec: Specification<T>,
    action: T.() -> Unit
): T = spec.write(this).apply(action)


/**
 * Update configuration using given specification
 */
public fun <T : Any> Configurable.update(
    spec: Specification<T>,
    action: T.() -> Unit,
): T = spec.write(config).apply(action)

//
//public fun  <M : MutableTypedMeta<M>> MutableMeta.withSpec(spec: Specification<M>): M? =
//    spec.write(it)

/**
 * A delegate that uses a [Specification] to wrap a child of this provider
 */
public fun <T : Scheme> MutableMeta.spec(
    spec: Specification<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return spec.write(getOrCreate(name))
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        set(name, value.toMeta())
    }
}

/**
 * A delegate that uses a [Specification] to wrap a list of child providers.
 * If children are mutable, the changes in list elements are reflected on them.
 * The list is a snapshot of children state, so change in structure is not reflected on its composition.
 */
@DFExperimental
public fun <T : Scheme> MutableMeta.listOfSpec(
    spec: Specification<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, List<T>> = object : ReadWriteProperty<Any?, List<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: property.name.asName()
        return getIndexed(name).values.map { spec.write(it as MutableMeta) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        val name = key ?: property.name.asName()
        setIndexed(name, value.map { it.toMeta() })
    }
}
