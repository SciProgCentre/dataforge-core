package hep.dataforge.context

import hep.dataforge.meta.*
import mu.KLogger
import mu.KotlinLogging
import kotlin.jvm.Synchronized
import kotlin.reflect.KClass

actual object Global: Context, JSContext("GLOBAL", null){
    /**
     * Closing all contexts
     *
     * @throws Exception
     */
    override fun close() {
        logger.info{"Shutting down GLOBAL"}
        for (ctx in contextRegistry.values) {
            ctx.close()
        }
        super.close()
    }

    private val contextRegistry = HashMap<String, Context>()

    /**
     * Get previously builder context o builder a new one
     *
     * @param name
     * @return
     */
    @Synchronized
    actual fun getContext(name: String): Context {
        return contextRegistry.getOrPut(name) { JSContext(name) }
    }
}

open class JSContext(
        final override val name: String,
        final override val parent: JSContext? = Global,
        properties: Meta = EmptyMeta
): Context {

    private val _properties = Config().apply { update(properties) }
    override val properties: Meta
        get() = if (parent == null) {
            _properties
        } else {
            Laminate(_properties, parent.properties)
        }

    override val plugins: PluginManager by lazy { PluginManager(this) }
    override val logger: KLogger = KotlinLogging.logger(name)

    /**
     * A property showing that dispatch thread is started in the context
     */
    private var started = false

    override fun <T : Any> services(type: KClass<T>): Sequence<T>  = TODO("Not implemented")

    /**
     * Free up resources associated with this context
     *
     * @throws Exception
     */
    override fun close() {
        if (isActive) error("Can't close active context")
        //detach all plugins
        plugins.forEach { it.detach() }
    }

    private val activators = HashSet<Any>()

    override val isActive: Boolean = !activators.isEmpty()

    override fun activate(activator: Any) {
        activators.add(activator)
    }

    override fun deactivate(activator: Any) {
        activators.clear()
    }
}