package space.kscience.dataforge.context

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.parseAsName
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.ThreadLocal

internal expect fun getGlobalLoggerFactory(): PluginFactory<out LogManager>

/**
 * A global root context. Closing [Global] terminates the framework.
 */
@ThreadLocal
private object GlobalContext : Context("GLOBAL".asName(), null, emptySet(), Meta.EMPTY) {
    override val coroutineContext: CoroutineContext = Job() + CoroutineName("GlobalContext")
}

public val Global: Context get() = GlobalContext

public fun Context(name: String? = null, block: ContextBuilder.() -> Unit = {}): Context =
    Global.buildContext(name?.parseAsName(), block)