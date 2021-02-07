package hep.dataforge.meta

import hep.dataforge.misc.DFBuilder
import hep.dataforge.names.Name
import kotlin.properties.ReadWriteProperty

/**
 * A container that holds a [Config].
 */
public interface Configurable {
    /**
     * Backing config
     */
    public val config: Config
}


public fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

@DFBuilder
public inline fun <T : Configurable> T.configure(action: Config.() -> Unit): T = apply { config.apply(action) }

/* Node delegates */

public fun Configurable.config(key: Name? = null): ReadWriteProperty<Any?, Config?> = config.node(key)
