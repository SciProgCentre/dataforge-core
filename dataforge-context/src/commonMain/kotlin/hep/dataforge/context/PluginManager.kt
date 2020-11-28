package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.provider.Type
import kotlin.reflect.KClass


@Type(PluginFactory.TYPE)
public interface PluginFactory<T : Plugin> : Factory<T> {
    public val tag: PluginTag
    public val type: KClass<out T>

    public companion object {
        public const val TYPE: String = "pluginFactory"
    }
}

/**
 * The manager for plugin system. Should monitor plugin dependencies and locks.
 *
 * @property context A context for this plugin manager
 * @author Alexander Nozik
 */
public class PluginManager(override val context: Context, plugins: Set<Plugin>) : ContextAware, Iterable<Plugin> {

    //TODO refactor to read-only container

    /**
     * A set of loaded plugins
     */
    private val plugins: HashSet<Plugin> = HashSet(plugins)

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
    public fun <T : Plugin> load(plugin: T): T {
        if (get(plugin::class, plugin.tag, recursive = false) != null) {
            error("Plugin with tag ${plugin.tag} already exists in ${context.name}")
        } else {
            for (tag in plugin.dependsOn()) {
                fetch(tag, true)
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
    public fun <T : Plugin> load(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY): T =
        load(factory(meta, context))

    public fun <T : Plugin> load(factory: PluginFactory<T>, metaBuilder: MetaBuilder.() -> Unit): T =
        load(factory, Meta(metaBuilder))

    /**
     * Remove a plugin from [PluginManager]
     */
    public fun remove(plugin: Plugin) {
        if (plugins.contains(plugin)) {
            logger.info { "Removing plugin ${plugin.name} from ${context.name}" }
            plugin.detach()
            plugins.remove(plugin)
        }
    }

    /**
     * Get an existing plugin with given meta or load new one using provided factory
     */
    public fun <T : Plugin> fetch(factory: PluginFactory<T>, recursive: Boolean = true, meta: Meta = Meta.EMPTY): T {
        val loaded = get(factory.type, factory.tag, recursive)
        return when {
            loaded == null -> load(factory(meta, context))
            loaded.meta == meta -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with tag ${factory.tag}. Plugin with this tag and different configuration already exists in context.")
        }
    }

    public fun <T : Plugin> fetch(
        factory: PluginFactory<T>,
        recursive: Boolean = true,
        metaBuilder: MetaBuilder.() -> Unit,
    ): T = fetch(factory, recursive, Meta(metaBuilder))

    override fun iterator(): Iterator<Plugin> = plugins.iterator()

}