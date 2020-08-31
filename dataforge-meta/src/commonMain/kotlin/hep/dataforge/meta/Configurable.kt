package hep.dataforge.meta

import hep.dataforge.meta.descriptors.*
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.toName
import hep.dataforge.values.Value
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A container that holds a [Config] and a default item provider.
 * Default item provider could be use for example to reference parent configuration.
 * It is not possible to know if some property is declared by provider just by looking on [Configurable],
 * this information should be provided externally.
 */
public interface Configurable : Described, MutableItemProvider {
    /**
     * Backing config
     */
    public val config: Config

    /**
     * Default meta item provider
     */
    public fun getDefaultItem(name: Name): MetaItem<*>? = null

    /**
     * Check if property with given [name] could be assigned to [item]
     */
    public fun validateItem(name: Name, item: MetaItem<*>?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validateItem(item) ?: true
    }

    override val descriptor: NodeDescriptor? get() = null

    /**
     * Get a property with default
     */
    override fun getItem(name: Name): MetaItem<*>? =
        config[name] ?: getDefaultItem(name) ?: descriptor?.get(name)?.defaultItem()

    /**
     * Set a configurable property
     */
    override fun setItem(name: Name, item: MetaItem<*>?) {
        if (validateItem(name, item)) {
            config.setItem(name, item)
        } else {
            error("Validation failed for property $name with value $item")
        }
    }
}

public fun Configurable.getItem(key: String): MetaItem<*>? = getItem(key.toName())

public fun Configurable.setItem(name: Name, value: Value?): Unit = setItem(name, value?.let { MetaItem.ValueItem(value) })
public fun Configurable.setItem(name: Name, meta: Meta?): Unit = setItem(name, meta?.let { MetaItem.NodeItem(meta) })

public fun Configurable.setItem(key: String, item: MetaItem<*>?): Unit = setItem(key.toName(), item)

public fun Configurable.setItem(key: String, value: Value?): Unit = setItem(key, value?.let { MetaItem.ValueItem(value) })
public fun Configurable.setItem(key: String, meta: Meta?): Unit = setItem(key, meta?.let { MetaItem.NodeItem(meta) })

public fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

@DFBuilder
public inline fun <T : Configurable> T.configure(action: Config.() -> Unit): T = apply { config.apply(action) }

/* Node delegates */

public fun Configurable.config(key: Name? = null): ReadWriteProperty<Any?, Config?> =
    config.node(key)

public fun MutableItemProvider.node(key: Name? = null): ReadWriteProperty<Any?, Meta?> = item(key).convert(
    reader = { it.node },
    writer = { it?.let { MetaItem.NodeItem(it) } }
)

public fun <T : Configurable> Configurable.spec(
    spec: Specification<T>, key: Name? = null
): ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: property.name.asName()
        return config[name].node?.let { spec.wrap(it) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: property.name.asName()
        config[name] = value?.config
    }
}

public fun <T : Configurable> Configurable.spec(
    spec: Specification<T>, default: T, key: Name? = null
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return config[name].node?.let { spec.wrap(it) } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        config[name] = value.config
    }
}

