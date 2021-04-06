package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaBuilder
import kotlin.reflect.KClass


/**
 * The manager for plugin system. Should monitor plugin dependencies and locks.
 *
 * @property context A context for this plugin manager
 * @author Alexander Nozik
 */
public class PluginManager(override val context: Context) : ContextAware, Iterable<Plugin> {

    /**
     * A set of loaded plugins
     */
    private val plugins: HashSet<Plugin> = HashSet()

    init {
        plugins.forEach { it.attach(context) }
    }

    /**
     * A [PluginManager] of parent context if it is present
     */
    private val parent: PluginManager? = context.parent?.plugins

    /**
     * List plugins stored in this [PluginManager]. If [inherit] is true, include parent plugins as well
     */
    public fun list(inherit: Boolean): Collection<Plugin> {
        return if (inherit && parent != null) {
            plugins + parent.list(true)
        } else {
            plugins
        }
    }

    /**
     * Get existing plugin or return null if not present. Only first matching plugin is returned.
     * @param inherit search for parent [PluginManager] plugins
     * @param predicate condition for the plugin
     */
    public fun find(inherit: Boolean = true, predicate: (Plugin) -> Boolean): Plugin? =
        list(inherit).find(predicate)

    /**
     * Find a loaded plugin via its tag
     *
     * @param tag
     * @return
     */
    public operator fun get(tag: PluginTag, inherit: Boolean = true): Plugin? =
        find(inherit) { tag.matches(it.tag) }

    /**
     * Find a loaded plugin via its class. This method does not check if the result is unique and just returns first
     * plugin matching the class condition.
     * For safe search provide a tag since tags are checked on load and plugins with the same tag are not allowed
     * in the same context.
     *
     * @param tag
     * @param type
     * @param <T>
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(type: KClass<out T>, tag: PluginTag? = null, recursive: Boolean = true): T? =
        find(recursive) { type.isInstance(it) && (tag == null || tag.matches(it.tag)) } as T?

    public inline operator fun <reified T : Any> get(tag: PluginTag? = null, recursive: Boolean = true): T? =
        get(T::class, tag, recursive)

    public inline operator fun <reified T : Plugin> get(factory: PluginFactory<T>, recursive: Boolean = true): T? =
        get(factory.type, factory.tag, recursive)

    /**
     * Load given plugin into this manager and return loaded instance.
     * Throw error if plugin of the same type and tag already exists in manager.
     *
     * @param plugin
     * @return
     */
    @Deprecated("Use immutable contexts instead")
    public fun <T : Plugin> load(plugin: T): T {
        if (get(plugin::class, plugin.tag, recursive = false) != null) {
            error("Plugin with tag ${plugin.tag} already exists in ${context.name}")
        } else {
            for ((factory, meta) in plugin.dependsOn()) {
                fetch(factory, meta, true)
            }

            logger.info { "Loading plugin ${plugin.name} into ${context.name}" }
            plugin.attach(context)
            plugins.add(plugin)
            return plugin
        }
    }

    /**
     * Load a plugin using its factory
     */
    @Deprecated("Use immutable contexts instead")
    public fun <T : Plugin> load(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY): T =
        load(factory(meta, context))

    @Deprecated("Use immutable contexts instead")
    public fun <T : Plugin> load(factory: PluginFactory<T>, metaBuilder: MetaBuilder.() -> Unit): T =
        load(factory, Meta(metaBuilder))

    /**
     * Remove a plugin from [PluginManager]
     */
    @Deprecated("Use immutable contexts instead")
    public fun remove(plugin: Plugin) {
        if (plugins.contains(plugin)) {
            Global.logger.info { "Removing plugin ${plugin.name} from ${context.name}" }
            plugin.detach()
            plugins.remove(plugin)
        }
    }

    /**
     * Get an existing plugin with given meta or load new one using provided factory
     */
    @Deprecated("Use immutable contexts instead")
    public fun <T : Plugin> fetch(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY, recursive: Boolean = true): T {
        val loaded = get(factory.type, factory.tag, recursive)
        return when {
            loaded == null -> load(factory(meta, context))
            loaded.meta == meta -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with tag ${factory.tag}. Plugin with this tag and different configuration already exists in context.")
        }
    }

    @Deprecated("Use immutable contexts instead")
    public fun <T : Plugin> fetch(
        factory: PluginFactory<T>,
        recursive: Boolean = true,
        metaBuilder: MetaBuilder.() -> Unit,
    ): T = fetch(factory, Meta(metaBuilder), recursive)

    override fun iterator(): Iterator<Plugin> = plugins.iterator()

}

/**
 * Fetch a plugin with given meta from the context. If the plugin (with given meta) is already registered, it is returned.
 * Otherwise, new child context with the plugin is created. In the later case the context could be retrieved from the plugin.
 */
public inline fun <reified T : Plugin> Context.fetch(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY): T {
    val existing = plugins[factory]
    return if (existing != null && existing.meta == meta) existing
    else {
        buildContext {
            plugin(factory, meta)
        }.plugins[factory]!!
    }
}