package space.kscience.dataforge.context

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.plus
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.provider.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
    plugins: Set<Plugin>, // set of unattached plugins
    meta: Meta,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : Named, MetaRepr, Provider, CoroutineScope {

    /**
     * Context properties. Working as a substitute for environment variables
     */
    public val properties: Laminate = if (parent == null) {
        Laminate(meta)
    } else {
        Laminate(meta, parent.properties)
    }


    /**
     * A [PluginManager] for current context
     */
    public val plugins: PluginManager by lazy { PluginManager(this, plugins) }

    override val defaultTarget: String get() = Plugin.TARGET

    public fun content(target: String, inherit: Boolean): Map<Name, Any> {
        return if (inherit) {
            when (target) {
                PROPERTY_TARGET -> properties.nodeSequence().toMap()
                Plugin.TARGET -> plugins.list(true).associateBy { it.name }
                else -> emptyMap()
            }
        } else {
            when (target) {
                PROPERTY_TARGET -> properties.layers.firstOrNull()?.nodeSequence()?.toMap() ?: emptyMap()
                Plugin.TARGET -> plugins.list(false).associateBy { it.name }
                else -> emptyMap()
            }
        }
    }

    override fun content(target: String): Map<Name, Any> = content(target, true)

    override val coroutineContext: CoroutineContext by lazy {
        (parent ?: Global).coroutineContext.let { parenContext ->
            parenContext + coroutineContext + SupervisorJob(parenContext[Job]) + CoroutineExceptionHandler { _, throwable ->
                logger.error(throwable) { "Exception in context $name" }
            }
        }
    }

    private val childrenContexts = HashMap<Name, Context>()

    /**
     * Build and register a new child context. If context with the same name, properties and plugins already exists, reuse it.
     * Throw an exception if context with the same name exists but is different.
     * @param name the relative (tail) name of the new context. If null, use context hash code as a marker.
     */
    public fun buildContext(
        name: String? = null,
        block: ContextBuilder.() -> Unit = {},
    ): Context = ContextBuilder(this, name).apply(block).build().also {
        val existing = childrenContexts[it.name]
        if (existing != null) {
            if (equals(existing, it)) {
                //reuse existing context if it is the same
                return existing
            } else {
                error("Context with name ${it.name} already exists but has different properties and plugins")
            }
        } else {
            childrenContexts[it.name] = it
        }
    }

    /**
     * Check if the current context already has plugins and properties matching the given block.
     * If it does, return it, otherwise create a new child context ensured to have them
     */
    public fun deriveContext(
        block: ContextBuilder.() -> Unit = {},
    ): Context {
        val builder = ContextBuilder(this, meta = properties).apply(block)

        val requiresFork = !Meta.equals(properties, builder.meta)
                || builder.factories.any { (factory, meta) ->
            val loaded = plugins[factory.tag]
            loaded == null || loaded.meta != meta
        }

        if (!requiresFork) return this

        // Search for existing child that adheres to restrictions
        childrenContexts.values.find { child ->
            Meta.equals(child.properties, builder.meta) && builder.factories.all { (factory, meta) ->
                val loaded = child.plugins[factory.tag]
                loaded != null && loaded.meta == meta
            }
        }?.let { return it }

        return buildContext(block = block)
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
        properties.layers.firstOrNull()?.let { set("properties", it) }
        "plugins" putIndexed plugins.map { it.toMeta() }
    }

    override fun toString(): String {
        val parentString = if (parent == Global) "" else ", parent=$parent"
        return "Context(name=$name$parentString)"
    }

    /**
     * A lazily initialized property that defines the serialization module for the current context.
     *
     * If plugin serializers modules are conflicting, throw a [kotlinx.serialization.modules.SerializerAlreadyRegisteredException].
     *
     * The module is constructed by combining the serialization modules provided
     * by the plugins associated with the context. If the context has a parent,
     * the parent's serialization module is combined with the plugins' serialization
     * modules. Plugin serializers are overwriting serializers from the parent.
     */
    public val serializationModule: SerializersModule by lazy {
        val pluginModules = plugins.mapNotNull { it.serializerModule }
        if (pluginModules.isEmpty()) {
            parent?.serializationModule ?: SerializersModule {}
        } else {
            val pluginModule = pluginModules.reduce { acc, module -> acc + module }
            parent?.serializationModule?.overwriteWith(pluginModule) ?: pluginModule
        }
    }


    /**
     * Json format for this context
     */
    public val json: Json by lazy {
        Json {
            prettyPrint = meta["json.prettyPrint"]?.boolean ?: true
            isLenient = meta["json.lenient"]?.boolean ?: true
            ignoreUnknownKeys = meta["json.ignoreUnknownKeys"]?.boolean ?: true
            serializersModule = serializationModule
        }
    }

    public companion object {
        public const val PROPERTY_TARGET: String = "context.property"

        internal fun equals(c1: Context, c2: Context): Boolean =
            c1.properties == c2.properties &&
                    c1.plugins.tags == c2.plugins.tags &&
                    c1.plugins.all { c2.plugins[it.tag]?.meta == it.meta }

    }
}

/**
 * Fetch a plugin with given meta from the context. If the plugin (with given meta) is already registered, it is returned.
 * Otherwise, new child context with the plugin is created. In the later case the context could be retrieved from the plugin.
 */
public inline fun <reified T : Plugin> Context.request(factory: PluginFactory<T>, meta: Meta? = null): T {
    return deriveContext {
        plugin(factory, meta ?: Meta.EMPTY)
    }.plugins[factory]!!
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