package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.forEach
import space.kscience.dataforge.data.map
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus

/**
 * Select data using given [selector]
 */
public suspend fun <T : Any> TaskResultBuilder<*>.from(
    selector: DataSelector<T>,
    meta: Meta = taskMeta
): DataSet<T> = selector.select(workspace, meta)


/**
 * Select data from a [WorkspacePlugin] attached to this [Workspace] context.
 */
public suspend inline fun <T : Any, reified P : WorkspacePlugin> TaskResultBuilder<*>.from(
    pluginFactory: PluginFactory<P>,
    meta: Meta = taskMeta,
    selectorBuilder: P.() -> TaskReference<T>,
): DataSet<T> {
    val plugin = workspace.context.plugins[pluginFactory]
        ?: error("Plugin ${pluginFactory.tag} not loaded into workspace context")
    val taskReference: TaskReference<T> = plugin.selectorBuilder()
    return workspace.produce(plugin.name + taskReference.taskName, meta) as TaskResult<T>
}

public val TaskResultBuilder<*>.allData: DataSelector<*>
    get() = object : DataSelector<Any> {
        override suspend fun select(workspace: Workspace, meta: Meta): DataSet<Any> = workspace.data
    }

/**
 * Perform a lazy mapping task using given [selector] and [action]. The meta of resulting
 * TODO move selector to receiver with multi-receivers
 */
@DFExperimental
public suspend inline fun <T : Any, reified R : Any> TaskResultBuilder<R>.pipeFrom(
    selector: DataSelector<T>,
    selectorMeta: Meta = taskMeta,
    dataMetaTransform: MutableMeta.() -> Unit = {},
    crossinline action: suspend (arg: T, name: Name, meta: Meta) -> R,
) {
    from(selector, selectorMeta).forEach { data ->
        val meta = data.meta.toMutableMeta().apply {
            taskName put taskMeta
            dataMetaTransform()
        }

        val res = data.map(workspace.context.coroutineContext, meta) {
            action(it, data.name, meta)
        }

        data(data.name, res)
    }
}


