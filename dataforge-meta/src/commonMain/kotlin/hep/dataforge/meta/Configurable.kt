package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * A container that holds a [Config] and a default item provider.
 * Default item provider could be use for example to reference parent configuration.
 * It is not possible to know if some property is declared by provider just by looking on [Configurable],
 * this information should be provided externally.
 */
interface Configurable {
    /**
     * Backing config
     */
    val config: Config

    /**
     * Default meta item provider
     */
    fun getDefaultItem(name: Name): MetaItem<*>? = null
}

/**
 * Get a property with default
 */
fun Configurable.getProperty(name: Name): MetaItem<*>? = config[name] ?: getDefaultItem(name)

fun Configurable.getProperty(key: String) = getProperty(key.toName())

/**
 * Set a configurable property
 */
fun Configurable.setProperty(name: Name, item: MetaItem<*>?) {
    config[name] = item
}

fun Configurable.setProperty(key: String, item: MetaItem<*>?) {
    setProperty(key.toName(), item)
}

fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

fun <T : Configurable> T.configure(action: Config.() -> Unit): T = apply { config.apply(action) }
