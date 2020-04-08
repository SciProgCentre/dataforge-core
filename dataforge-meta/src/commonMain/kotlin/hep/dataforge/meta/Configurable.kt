package hep.dataforge.meta

import hep.dataforge.meta.descriptors.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.values.Value

/**
 * A container that holds a [Config] and a default item provider.
 * Default item provider could be use for example to reference parent configuration.
 * It is not possible to know if some property is declared by provider just by looking on [Configurable],
 * this information should be provided externally.
 */
interface Configurable : Described {
    /**
     * Backing config
     */
    val config: Config

    /**
     * Default meta item provider
     */
    fun getDefaultItem(name: Name): MetaItem<*>? = null

    /**
     * Check if property with given [name] could be assigned to [value]
     */
    fun validateItem(name: Name, item: MetaItem<*>?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validateItem(item) ?: true
    }

    override val descriptor: NodeDescriptor? get() = null

    /**
     * Get a property with default
     */
    fun getProperty(name: Name): MetaItem<*>? =
        config[name] ?: getDefaultItem(name) ?: descriptor?.get(name)?.defaultItem()

    /**
     * Set a configurable property
     */
    fun setProperty(name: Name, item: MetaItem<*>?) {
        if (validateItem(name, item)) {
            config[name] = item
        } else {
            error("Validation failed for property $name with value $item")
        }
    }
}

fun Configurable.getProperty(key: String) = getProperty(key.toName())

fun Configurable.setProperty(name: Name, value: Value?) = setProperty(name, value?.let { MetaItem.ValueItem(value) })
fun Configurable.setProperty(name: Name, meta: Meta?) = setProperty(name, meta?.let { MetaItem.NodeItem(meta) })

fun Configurable.setProperty(key: String, item: MetaItem<*>?) {
    setProperty(key.toName(), item)
}

fun Configurable.setProperty(key: String, value: Value?) = setProperty(key, value?.let { MetaItem.ValueItem(value) })
fun Configurable.setProperty(key: String, meta: Meta?) = setProperty(key, meta?.let { MetaItem.NodeItem(meta) })

fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

inline fun <T : Configurable> T.configure(action: Config.() -> Unit): T = apply { config.apply(action) }
