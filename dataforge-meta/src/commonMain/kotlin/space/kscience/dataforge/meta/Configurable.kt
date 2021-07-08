package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.names.Name
import kotlin.properties.ReadWriteProperty

/**
 * A container that holds a [ObservableMeta].
 */
public interface Configurable {
    /**
     * Backing config
     */
    public val config: ObservableMeta
}


public fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

@DFBuilder
public inline fun <T : Configurable> T.configure(action: ObservableMeta.() -> Unit): T = apply { config.apply(action) }

/* Node delegates */

public fun Configurable.config(key: Name? = null): ReadWriteProperty<Any?, ObservableMeta?> = config.node(key)
