package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.names.asName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.ThreadLocal

internal expect val globalLoggerFactory: PluginFactory<out LogManager>

/**
 * A global root context. Closing [Global] terminates the framework.
 */
@ThreadLocal
public object Global : Context("GLOBAL".asName(), null, Meta.EMPTY) {

    override val coroutineContext: CoroutineContext = GlobalScope.coroutineContext + SupervisorJob()

    /**
     * The default logging manager
     */
    public val logger: LogManager by lazy { globalLoggerFactory.invoke(context = this).apply { attach(this@Global) } }

    /**
     * Closing all contexts
     *
     * @throws Exception
     */
    override fun close() {
        logger.info { "Shutting down GLOBAL" }
        for (ctx in contextRegistry.values) {
            ctx.close()
        }
        super.close()
    }

    private val contextRegistry = HashMap<String, Context>()

    /**
     * Get previously built context
     *
     * @param name
     * @return
     */
    public fun getContext(name: String): Context? {
        return contextRegistry[name]
    }

    public fun context(name: String, parent: Context = this, block: ContextBuilder.() -> Unit = {}): Context =
        ContextBuilder(parent, name).apply(block).build().also {
            contextRegistry[name] = it
        }

}