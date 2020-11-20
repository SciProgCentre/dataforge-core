package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import kotlin.jvm.JvmName
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
    public fun read(meta: Meta, defaultProvider: ItemProvider = ItemProvider.EMPTY): T

    /**
     * Wrap mutable [Config], using it as inner storage (changes to [Specification] are reflected on [Config]
     */
    public fun wrap(config: Config, defaultProvider: ItemProvider = ItemProvider.EMPTY): T =
        read(config as Meta, defaultProvider)

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
 * Update given configuration using given type as a builder
 */
public fun <T : MutableItemProvider> Specification<T>.update(meta: Meta, action: T.() -> Unit): T =
    read(meta).apply(action)


/**
 * Apply specified configuration to configurable
 */
public fun <T : MetaRepr, C : MutableItemProvider, S : Specification<C>> T.configure(spec: S, action: C.() -> Unit): T =
    apply { spec.update(toMeta(), action) }

/**
 * Update configuration using given specification
 */
public fun <C : MutableItemProvider, S : Specification<C>> Configurable.update(
    spec: S,
    action: C.() -> Unit,
): Configurable =
    apply { spec.update(config, action) }

/**
 * Create a style based on given specification
 */
public fun <C : MutableItemProvider, S : Specification<C>> S.createStyle(action: C.() -> Unit): Meta =
    Config().also { update(it, action) }

public fun <T : MutableItemProvider> MetaItem<*>.spec(spec: Specification<T>): T? = node?.let {
    spec.wrap(
        Config(), it
    )
}

@JvmName("configSpec")
public fun <T : MutableItemProvider> MetaItem<Config>.spec(spec: Specification<T>): T? = node?.let { spec.wrap(it) }

public fun <T : Scheme> MutableItemProvider.spec(
    spec: Specification<T>, key: Name? = null,
): ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: property.name.asName()
        return getItem(name).node?.let { spec.read(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: property.name.asName()
        setItem(name, value?.toMeta()?.asMetaItem())
    }
}

public fun <T : Scheme> MutableItemProvider.spec(
    spec: Specification<T>,
    default: T,
    key: Name? = null,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return getItem(name).node?.let { spec.read(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        setItem(name, value.toMeta().asMetaItem())
    }
}