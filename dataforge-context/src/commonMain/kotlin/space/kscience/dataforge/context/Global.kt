package space.kscience.dataforge.context

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.asName
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.ThreadLocal

internal expect val globalLoggerFactory: PluginFactory<out LogManager>

/**
 * A global root context. Closing [Global] terminates the framework.
 */
@ThreadLocal
private object GlobalContext : Context("GLOBAL".asName(), null, Meta.EMPTY) {
    override val coroutineContext: CoroutineContext = GlobalScope.coroutineContext + Job()
}

public val Global: Context get() = GlobalContext

public fun Context(name: String? = null, block: ContextBuilder.() -> Unit = {}): Context =
    Global.buildContext(name, block)