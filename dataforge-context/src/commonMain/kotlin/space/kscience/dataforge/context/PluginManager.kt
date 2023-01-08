package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.plus
import kotlin.reflect.KClass


/**
 * The manager for plugin system. Should monitor plugin dependencies and locks.
 *
 * @property context A context for this plugin manager
 * @author Alexander Nozik
 */
public class PluginManager internal constructor(
    override val context: Context,
    private val plugins: Set<Plugin>,
) : ContextAware, Iterable<Plugin> {

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

    override fun iterator(): Iterator<Plugin> = plugins.iterator()
}

/**
 * Fetch a plugin with given meta from the context. If the plugin (with given meta) is already registered, it is returned.
 * Otherwise, new child context with the plugin is created. In the later case the context could be retrieved from the plugin.
 */
public inline fun <reified T : Plugin> Context.request(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY): T {
    val existing = plugins[factory]
    return if (existing != null && existing.meta == meta) existing
    else {
        buildContext(name = this@request.name + factory.tag.name) {
            plugin(factory, meta)
        }.plugins[factory]!!
    }
}

@Deprecated("Replace with request", ReplaceWith("request(factory, meta)"))
public inline fun <reified T : Plugin> Context.fetch(factory: PluginFactory<T>, meta: Meta = Meta.EMPTY): T =
    request(factory, meta)