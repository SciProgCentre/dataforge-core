package space.kscience.dataforge.context

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.ThreadLocal

internal expect fun getGlobalLoggerFactory(): PluginFactory<out LogManager>

/**
 * A global root context. Closing [Global] terminates the framework.
 */
@ThreadLocal
private object GlobalContext : Context(Name.of("GLOBAL"), null, emptySet(), Meta.EMPTY) {
    override val coroutineContext: CoroutineContext = Job() + CoroutineName("GlobalContext")
}

public val Global: Context get() = GlobalContext

/**
 * Create a new context with given configuration [block]. Could reuse an existing context if [name] is null and
 * the created context in identical to the existing one.
 */
public fun Context(
    name: String? = null,
    block: ContextBuilder.() -> Unit = {}
): Context = Global.buildContext(name, block = block)
