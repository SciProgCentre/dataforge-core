package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Allows to apply custom configuration in a type safe way to simple untyped configuration.
 * By convention [Scheme] companion should inherit this class
 *
 */
public interface Specification<T : MutableItemProvider> {
    /**
     * Read generic read-only meta with this [Specification] producing instance of desired type.
     */
    public fun read(items: ItemProvider): T

    /**
     * Wrap [MutableItemProvider], using it as inner storage (changes to [Specification] are reflected on [MutableItemProvider]
     */
    public fun write(target: MutableItemProvider, defaultProvider: ItemProvider = ItemProvider.EMPTY): T

    /**
     * Generate an empty object
     */
    public fun empty(): T = read(Meta.EMPTY)

    /**
     * A convenience method to use specifications in builders
     */
    public operator fun invoke(action: T.() -> Unit): T = empty().apply(action)
}

/**
 * Update a [Config] using given specification
 */
public fun <T : MutableItemProvider> Config.update(spec: Specification<T>, action: T.() -> Unit): T =
    spec.write(this).apply(action)

/**
 * Update configuration using given specification
 */
public fun <C : MutableItemProvider, S : Specification<C>> Configurable.update(
    spec: S,
    action: C.() -> Unit,
): Configurable = apply { config.update(spec, action) }

public fun <T : MutableItemProvider> TypedMetaItem<Config>.withSpec(spec: Specification<T>): T? =
    node?.let { spec.write(it) }

public fun <T : Scheme> MutableItemProvider.spec(
    spec: Specification<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: property.name.asName()
        return get(name).node?.let { spec.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: property.name.asName()
        set(name, value?.toMeta()?.asMetaItem())
    }
}

public fun <T : Scheme> MutableItemProvider.spec(
    spec: Specification<T>,
    default: T,
    key: Name? = null,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return get(name).node?.let { spec.read(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        set(name, value.toMeta().asMetaItem())
    }
}