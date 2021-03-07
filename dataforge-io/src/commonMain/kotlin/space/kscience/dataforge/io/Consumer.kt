package space.kscience.dataforge.io

import space.kscience.dataforge.misc.DFExperimental

/**
 * A fire-and-forget consumer of messages
 */
@DFExperimental
public interface Consumer {
    public fun consume(message: Envelope): Unit
}