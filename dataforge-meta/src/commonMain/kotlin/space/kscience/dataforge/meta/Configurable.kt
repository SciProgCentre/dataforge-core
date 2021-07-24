package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFBuilder

/**
 * A container that holds a [ObservableMeta].
 */
public interface Configurable {
    /**
     * Backing config
     */
    public val config: MutableMeta
}


public fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

@DFBuilder
public inline fun <T : Configurable> T.configure(action: MutableMeta.() -> Unit): T = apply { config.apply(action) }