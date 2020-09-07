package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import kotlin.reflect.KClass


interface PluginFactory<T : Plugin> : Factory<T> {
    val tag: PluginTag
    val type: KClass<out T>

    companion object{
        const val TYPE = "pluginFactory"
    }
}


/**
 * The manager for plugin system. Should monitor plugin dependencies and locks.
 *
 * @property context A context for this plugin manager
 * @author Alexander Nozik
 */
class PluginManager(override val context: Context) : ContextAware, Iterable<Plugin> {

    /**
     * A set of loaded plugins
     */
    private val plugins = HashSet<Plugin>()

    /**
     * A [PluginManager] of parent context if it is present
     */
    private val parent: PluginManager? = context.parent?.plugins


    fun sequence(recursive: Boolean): Sequence<Plugin> {
        return if (recursive && parent != null) {
            plugins.asSequence() + parent.sequence(true)
        } else {
            plugins.asSequence()
        }
    }

    /**
     * Get existing plugin or return null if not present. Only first matching plugin is returned.
     * @param recursive search for parent [PluginManager] plugins
     * @param predicate condition for the plugin
     */
    fun find(recursive: Boolean = true, predicate: (Plugin) -> Boolean): Plugin? = sequence(recursive).find(predicate)


    /**
     * Find a loaded plugin via its tag
     *
     * @param tag
     * @return
     */
    operator fun get(tag: PluginTag, recursive: Boolean = true): Plugin? = find(recursive) { tag.matches(it.tag) }


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
    operator fun <T : Any> get(type: KClass<out T>, tag: PluginTag? = null, recursive: Boolean = true): T? =
        find(recursive) { type.isInstance(it) && (tag == null || tag.matches(it.tag)) } as T?

    inline operator fun <reified T : Any> get(tag: PluginTag? = null, recursive: Boolean = true): T? =
        get(T::class, tag, recursive)

    inline operator fun <reified T : Plugin> get(factory: PluginFactory<T>, recursive: Boolean = true): T? =
        get(factory.type, factory.tag, recursive)

    /**
     * Load given plugin into this manager and return loaded instance.
     * Throw error if plugin of the same type and tag already exists in manager.
     *
     * @param plugin
     * @return
     */
    fun <T : Plugin> load(plugin: T): T {
        if (context.isActive) error("Can't load plugin into active context")

        if (get(plugin::class, plugin.tag, recursive = false) != null) {
            error("Plugin of type ${plugin::class} already exists in ${context.name}")
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
    fun <T : Plugin> load(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY): T =
        load(factory(meta, context))

    fun <T : Plugin> load(factory: PluginFactory<T>, metaBuilder: MetaBuilder.() -> Unit): T =
        load(factory, Meta(metaBuilder))

    /**
     * Remove a plugin from [PluginManager]
     */
    fun remove(plugin: Plugin) {
        if (context.isActive) error("Can't remove plugin from active context")

        if (plugins.contains(plugin)) {
            logger.info { "Removing plugin ${plugin.name} from ${context.name}" }
            plugin.detach()
            plugins.remove(plugin)
        }
    }

    /**
     * Get an existing plugin with given meta or load new one using provided factory
     *
     */
    fun <T : Plugin> fetch(factory: PluginFactory<T>, recursive: Boolean = true, meta: Meta = Meta.EMPTY): T {
        val loaded = get(factory.type, factory.tag, recursive)
        return when {
            loaded == null -> load(factory(meta, context))
            loaded.meta == meta -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with tag ${factory.tag}. Plugin with this tag and different configuration already exists in context.")
        }
    }

    fun <T : Plugin> fetch(
        factory: PluginFactory<T>,
        recursive: Boolean = true,
        metaBuilder: MetaBuilder.() -> Unit
    ): T = fetch(factory, recursive, Meta(metaBuilder))

    override fun iterator(): Iterator<Plugin> = plugins.iterator()

}