package space.kscience.dataforge.output

import space.kscience.dataforge.context.*
import space.kscience.dataforge.context.PluginTag.Companion.DATAFORGE_GROUP
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

/**
 * A manager for outputs
 */
public interface OutputManager {

    /**
     * Get an output specialized for given type, name and stage.
     * @param stage represents the node or directory for the output. Empty means root node.
     * @param name represents the name inside the node.
     * @param meta configuration for [Renderer] (not for rendered object)
     */
    public fun <T : Any> getOutputContainer(
        type: KClass<out T>,
        name: Name,
        stage: Name = Name.EMPTY,
        meta: Meta = Meta.EMPTY
    ): Renderer<T>
}

/**
 * Get an output manager for a context
 */
public val Context.output: OutputManager get() = plugins.get() ?: ConsoleOutputManager()

/**
 * Get an output with given [name], [stage] and reified content type
 */
public inline fun <reified T : Any> OutputManager.getOutputContainer(
    name: Name,
    stage: Name = Name.EMPTY,
    meta: Meta = Meta.EMPTY
): Renderer<T> {
    return getOutputContainer(T::class, name, stage, meta)
}

/**
 * Directly render an object using the most suitable renderer
 */
public fun OutputManager.render(obj: Any, name: Name, stage: Name = Name.EMPTY, meta: Meta = Meta.EMPTY): Unit =
    getOutputContainer(obj::class, name, stage).render(obj, meta)

/**
 * System console output.
 * The [CONSOLE_RENDERER] is used when no other [OutputManager] is provided.
 */
public val CONSOLE_RENDERER: Renderer<Any> = Renderer { obj, meta -> println(obj) }

public class ConsoleOutputManager : AbstractPlugin(), OutputManager {
    override val tag: PluginTag get() = ConsoleOutputManager.tag

    override fun <T : Any> getOutputContainer(type: KClass<out T>, name: Name, stage: Name, meta: Meta): Renderer<T> = CONSOLE_RENDERER

    public companion object : PluginFactory<ConsoleOutputManager> {
        override val tag: PluginTag = PluginTag("output.console", group = DATAFORGE_GROUP)

        override val type: KClass<ConsoleOutputManager> = ConsoleOutputManager::class

        override fun invoke(meta: Meta, context: Context): ConsoleOutputManager = ConsoleOutputManager()
    }
}

/**
 * A dispatcher for output tasks.
 */
public expect val Dispatchers.Output: CoroutineDispatcher