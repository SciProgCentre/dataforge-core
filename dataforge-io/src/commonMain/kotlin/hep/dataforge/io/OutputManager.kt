package hep.dataforge.io

import hep.dataforge.context.Plugin
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.EmptyName
import hep.dataforge.names.Name
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

inline fun <reified T : Any> OutputManager.typed(name: Name, stage: Name = EmptyName, meta: Meta = EmptyMeta): Output<T> {
    return typed(T::class, name, stage, meta)
}
