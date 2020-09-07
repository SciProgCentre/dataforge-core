package hep.dataforge.context

import hep.dataforge.names.asName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * A global root context. Closing [Global] terminates the framework.
 */
public object Global : Context("GLOBAL".asName(), null) {

    override val coroutineContext: CoroutineContext = GlobalScope.coroutineContext + SupervisorJob()

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
        ContextBuilder(parent, name).apply(block).build()

}