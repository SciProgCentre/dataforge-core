package hep.dataforge.context

import hep.dataforge.misc.Named
import hep.dataforge.provider.Path

/**
 * Part of kotlin-logging interface
 */
public expect interface Logger {
    public fun trace(msg: () -> Any?)
    public fun debug(msg: () -> Any?)
    public fun info(msg: () -> Any?)
    public fun warn(msg: () -> Any?)
    public fun error(msg: () -> Any?)
}

public expect fun Context.buildLogger(name: String): Logger

/**
 * The logger specific to this context
 */
public val Context.logger: Logger get() = buildLogger(name.toString())

/**
 * The logger
 */
public val ContextAware.logger: Logger
    get() = if (this is Named) {
        context.buildLogger(Path(context.name, this.name).toString())
    } else {
        context.logger
    }

