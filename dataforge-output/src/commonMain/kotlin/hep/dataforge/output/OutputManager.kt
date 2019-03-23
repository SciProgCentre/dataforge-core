package hep.dataforge.output

import hep.dataforge.context.*
import hep.dataforge.context.PluginTag.Companion.DATAFORGE_GROUP
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.EmptyName
import hep.dataforge.names.Name
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

/**
 * A manager for outputs
 */
interface OutputManager : Plugin {

    /**
     * Get an output specialized for given type, name and stage.
     * @param stage represents the node or directory for the output. Empty means root node.
     * @param name represents the name inside the node.
     * @param meta configuration for [Output] (not for rendered object)
     */
    operator fun <T : Any> get(
        type: KClass<out T>,
        name: Name,
        stage: Name = EmptyName,
        meta: Meta = EmptyMeta
    ): Output<T>
}

/**
 * Get an output manager for a context
 */
val Context.output: OutputManager get() = plugins.get() ?: ConsoleOutputManager()

/**
 * Get an output with given [name], [stage] and reified content type
 */
inline operator fun <reified T : Any> OutputManager.get(
    name: Name,
    stage: Name = EmptyName,
    meta: Meta = EmptyMeta
): Output<T> {
    return get(T::class, name, stage, meta)
}

/**
 * Directly render an object using the most suitable renderer
 */
fun OutputManager.render(obj: Any, name: Name, stage: Name = EmptyName, meta: Meta = EmptyMeta) =
    get(obj::class, name, stage).render(obj, meta)

/**
 * System console output.
 * The [ConsoleOutput] is used when no other [OutputManager] is provided.
 */
expect val ConsoleOutput: Output<Any>

class ConsoleOutputManager : AbstractPlugin(), OutputManager {
    override val tag: PluginTag get() = ConsoleOutputManager.tag

    override fun <T : Any> get(type: KClass<out T>, name: Name, stage: Name, meta: Meta): Output<T> = ConsoleOutput

    companion object : PluginFactory<ConsoleOutputManager> {
        override val tag = PluginTag("output.console", group = DATAFORGE_GROUP)

        override val type = ConsoleOutputManager::class

        override fun build() = ConsoleOutputManager()
    }
}

/**
 * A dispatcher for output tasks.
 */
expect val Dispatchers.Output: CoroutineDispatcher