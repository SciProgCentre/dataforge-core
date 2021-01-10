package hep.dataforge.context

import hep.dataforge.misc.Named
import hep.dataforge.provider.Path
import mu.KLogger
import mu.KotlinLogging

/**
 * The logger specific to this context
 */
public val Context.logger: KLogger get() = KotlinLogging.logger(name.toString())

/**
 * The logger
 */
public val ContextAware.logger: KLogger
    get() = if (this is Named) {
        KotlinLogging.logger(Path(context.name, this.name).toString())
    } else {
        context.logger
    }

