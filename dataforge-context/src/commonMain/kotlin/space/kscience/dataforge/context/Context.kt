package space.kscience.dataforge.context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.meta.itemSequence
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.provider.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Synchronized

/**
 * The local environment for anything being done in DataForge framework. Contexts are organized into tree structure with [Global] at the top.
 * Context has [properties] - equivalent for system environment values, but grouped into a tree and inherited from parent context.
 *
 * The main function of the Context is to provide [PluginManager] which stores the loaded plugins and works as a dependency injection point.
 * The normal behaviour of the [PluginManager] is to search for a plugin in parent context if it is not found in a current one. It is possible to have
 * different plugins with the same interface in different contexts in the hierarchy. The usual behaviour is to use nearest one, but it could
 * be overridden by plugin implementation.
 *
 */
public open class Context internal constructor(
    final override val name: Name,
    public val parent: Context?,
    meta: Meta,
) : Named, MetaRepr, Provider, CoroutineScope {

    /**
     * Context properties. Working as substitute for environment variables
     */
    public val properties: Laminate = if (parent == null) {
        Laminate(meta)
    } else {
        Laminate(meta, parent.properties)
    }


    /**
     * A [PluginManager] for current context
     */
    public val plugins: PluginManager by lazy { PluginManager(this) }

    override val defaultTarget: String get() = Plugin.TARGET

    public fun content(target: String, inherit: Boolean): Map<Name, Any> {
        return if (inherit) {
            when (target) {
                PROPERTY_TARGET -> properties.itemSequence().toMap()
                Plugin.TARGET -> plugins.list(true).associateBy { it.name }
                else -> emptyMap()
            }
        } else {
            when (target) {
                PROPERTY_TARGET -> properties.layers.firstOrNull()?.itemSequence()?.toMap() ?: emptyMap()
                Plugin.TARGET -> plugins.list(false).associateBy { it.name }
                else -> emptyMap()
            }
        }
    }

    override fun content(target: String): Map<Name, Any> = content(target, true)

    override val coroutineContext: CoroutineContext by lazy {
        (parent ?: Global).coroutineContext.let { parenContext ->
            parenContext + SupervisorJob(parenContext[Job])
        }
    }

    private val childrenContexts = HashMap<Name, Context>()

    /**
     * Build and register a child context
     */
    @Synchronized
    public fun buildContext(name: String? = null, block: ContextBuilder.() -> Unit): Context {
        val newContext = ContextBuilder(this)
            .apply { name?.let { name(it) } }
            .apply(block)
            .build()
        childrenContexts[newContext.name] = newContext
        return newContext
    }

    /**
     * Detach all plugins, and close child contexts
     */
    public open fun close() {
        //recursively closed child context
        childrenContexts.forEach { it.value.close() }
        //detach all plugins
        plugins.forEach { it.detach() }
    }

    override fun toMeta(): Meta = Meta {
        "parent" to parent?.name
        "properties" put properties.layers.firstOrNull()
        "plugins" put plugins.map { it.toMeta() }
    }

    public companion object {
        public const val PROPERTY_TARGET: String = "context.property"
    }
}

/**
 * The interface for something that encapsulated in context
 *
 */
public interface ContextAware {
    /**
     * Get context for this object
     *
     * @return
     */
    public val context: Context
}