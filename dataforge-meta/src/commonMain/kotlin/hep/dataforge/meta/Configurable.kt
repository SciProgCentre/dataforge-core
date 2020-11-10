package hep.dataforge.meta

import hep.dataforge.names.Name
import kotlin.properties.ReadWriteProperty

/**
 * A container that holds a [Config].
 */
public interface Configurable : MutableItemProvider {
    /**
     * Backing config
     */
    public val config: Config

    /**
     * Get a property with default
     */
    override fun getItem(name: Name): MetaItem<*>? = config[name]

    /**
     * Set a configurable property
     */
    override fun setItem(name: Name, item: MetaItem<*>?) {
        config.setItem(name, item)
    }
}


public fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

@DFBuilder
public inline fun <T : Configurable> T.configure(action: Config.() -> Unit): T = apply { config.apply(action) }

/* Node delegates */

public fun Configurable.config(key: Name? = null): ReadWriteProperty<Any?, Config?> = config.node(key)
