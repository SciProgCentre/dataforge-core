package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import kotlin.reflect.KClass

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
    fun get(recursive: Boolean = true, predicate: (Plugin) -> Boolean): Plugin? = sequence(recursive).find(predicate)


    /**
     * Find a loaded plugin via its tag
     *
     * @param tag
     * @return
     */
    operator fun get(tag: PluginTag, recursive: Boolean = true): Plugin? = get(recursive) { tag.matches(it.tag) }


    /**
     * Find a loaded plugin via its class
     *
     * @param tag
     * @param type
     * @param <T>
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Plugin> get(type: KClass<T>, recursive: Boolean = true): T? =
        get(recursive) { type.isInstance(it) } as T?

    inline fun <reified T : Plugin> get(recursive: Boolean = true): T? = get(T::class, recursive)


    /**
     * Load given plugin into this manager and return loaded instance.
     * Throw error if plugin of the same class already exists in manager
     *
     * @param plugin
     * @return
     */
    fun <T : Plugin> load(plugin: T): T {
        if (context.isActive) error("Can't load plugin into active context")

        if (get(plugin::class, false) != null) {
            throw  RuntimeException("Plugin of type ${plugin::class} already exists in ${context.name}")
        } else {
            loadDependencies(plugin)

            logger.info { "Loading plugin ${plugin.name} into ${context.name}" }
            plugin.attach(context)
            plugins.add(plugin)
            return plugin
        }
    }

    private fun loadDependencies(plugin: Plugin) {
        for (tag in plugin.dependsOn()) {
            load(tag)
        }
    }

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
     * Get plugin instance via plugin resolver and load it.
     *
     * @param tag
     * @return
     */
    fun load(tag: PluginTag, meta: Meta = EmptyMeta): Plugin {
        val loaded = get(tag, false)
        return when {
            loaded == null -> load(PluginRepository.fetch(tag, meta))
            loaded.meta == meta -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with tag $tag. Plugin with this tag and different configuration already exists in context.")
        }
    }

    /**
     * Load plugin by its class and meta. Ignore if plugin with this meta is already loaded.
     * Throw an exception if there exists plugin with the same type, but different meta
     */
    fun <T : Plugin> load(type: KClass<T>, meta: Meta = EmptyMeta): T {
        val loaded = get(type, false)
        return when {
            loaded == null -> {
                val plugin = PluginRepository.list().first { it.type == type }.build(meta)
                if (type.isInstance(plugin)) {
                    @Suppress("UNCHECKED_CAST")
                    load(plugin as T)
                } else {
                    error("Corrupt type information in plugin repository")
                }
            }
            loaded.meta == meta -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with type $type. Plugin with this type and different configuration already exists in context.")
        }
    }

    inline fun <reified T : Plugin> load(noinline metaBuilder: MetaBuilder.() -> Unit = {}): T {
        return load(T::class, buildMeta(metaBuilder))
    }

    fun load(name: String, meta: Meta = EmptyMeta): Plugin {
        return load(PluginTag.fromString(name), meta)
    }

    override fun iterator(): Iterator<Plugin> = plugins.iterator()

    /**
     * Get a plugin if it exists or load it with given meta if it is not.
     */
    inline fun <reified T : Plugin> getOrLoad(noinline metaBuilder: MetaBuilder.() -> Unit = {}): T {
        return get(true) ?: load(metaBuilder)
    }

}