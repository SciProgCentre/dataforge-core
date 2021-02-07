package hep.dataforge.io

import hep.dataforge.misc.DFExperimental

/**
 * A fire-and-forget consumer of messages
 */
@DFExperimental
public interface Consumer {
    public fun consume(message: Envelope): Unit
}