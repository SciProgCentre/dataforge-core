package hep.dataforge.io

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.context.Plugin
import hep.dataforge.context.PluginTag
import hep.dataforge.context.PluginTag.Companion.DATAFORGE_GROUP
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.EmptyName
import hep.dataforge.names.Name
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

/**
 * A manager for outputs
 */
interface OutputManager : Plugin {
    /**
     * Provide an output for given name and stage.
     *
     * @param stage represents the node or directory for the output. Empty means root node.
     * @param name represents the name inside the node.
     * @param meta configuration for [Output] (not for rendered object)
     *
     */
    operator fun get(name: Name, stage: Name = EmptyName, meta: Meta = EmptyMeta): Output<Any>

    /**
     * Get an output specialized for giver ntype
     */
    fun <T : Any> typed(type: KClass<T>, name: Name, stage: Name = EmptyName, meta: Meta = EmptyMeta): Output<T>

}

/**
 * Get an output with given [name], [stage] and reified content type
 */
inline fun <reified T : Any> OutputManager.typed(
    name: Name,
    stage: Name = EmptyName,
    meta: Meta = EmptyMeta
): Output<T> {
    return typed(T::class, name, stage, meta)
}

/**
 * System console output.
 * The [ConsoleOutput] is used when no other [OutputManager] is provided.
 */
expect val ConsoleOutput: Output<Any>

object ConsoleOutputManager : AbstractPlugin(), OutputManager {
    override val tag: PluginTag = PluginTag("output.console", group = DATAFORGE_GROUP)

    override fun get(name: Name, stage: Name, meta: Meta): Output<Any> = ConsoleOutput

    override fun <T : Any> typed(type: KClass<T>, name: Name, stage: Name, meta: Meta): Output<T> = ConsoleOutput
}

/**
 * A dispatcher for output tasks.
 */
expect val OutputDispatcher : CoroutineDispatcher