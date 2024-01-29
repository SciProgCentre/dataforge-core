package space.kscience.dataforge.workspace

import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.branch
import space.kscience.dataforge.data.forEach
import space.kscience.dataforge.data.transform
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus

/**
 * A task meta without a node corresponding to the task itself (removing a node with name of the task).
 */
public val TaskResultBuilder<*>.defaultDependencyMeta: Meta
    get() = taskMeta.copy {
        remove(taskName)
    }

/**
 * Select data using given [selector]
 *
 * @param selector a workspace data selector. Could be either task selector or initial data selector.
 * @param dependencyMeta meta used for selector. The same meta is used for caching. By default, uses [defaultDependencyMeta].
 */
public suspend fun <T> TaskResultBuilder<*>.from(
    selector: DataSelector<T>,
    dependencyMeta: Meta = defaultDependencyMeta,
): DataTree<T> = selector.select(workspace, dependencyMeta)

public suspend inline fun <T, reified P : WorkspacePlugin> TaskResultBuilder<*>.from(
    plugin: P,
    dependencyMeta: Meta = defaultDependencyMeta,
    selectorBuilder: P.() -> TaskReference<T>,
): TaskResult<T> {
    require(workspace.context.plugins.contains(plugin)) { "Plugin $plugin is not loaded into $workspace" }
    val taskReference: TaskReference<T> = plugin.selectorBuilder()
    val res = workspace.produce(plugin.name + taskReference.taskName, dependencyMeta)
    //TODO add explicit check after https://youtrack.jetbrains.com/issue/KT-32956
    @Suppress("UNCHECKED_CAST")
    return res as TaskResult<T>
}

/**
 * Select data from a [WorkspacePlugin] attached to this [Workspace] context.
 *
 * @param pluginFactory a plugin which contains the task definition. The plugin must be loaded into Workspace context.
 * @param dependencyMeta meta used for selector. The same meta is used for caching. By default, uses [defaultDependencyMeta].
 * @param selectorBuilder a builder of task from the plugin.
 */
public suspend inline fun <reified T, reified P : WorkspacePlugin> TaskResultBuilder<*>.from(
    pluginFactory: PluginFactory<P>,
    dependencyMeta: Meta = defaultDependencyMeta,
    selectorBuilder: P.() -> TaskReference<T>,
): TaskResult<T> {
    val plugin = workspace.context.plugins[pluginFactory]
        ?: error("Plugin ${pluginFactory.tag} not loaded into workspace context")
    val taskReference: TaskReference<T> = plugin.selectorBuilder()
    val res = workspace.produce(plugin.name + taskReference.taskName, dependencyMeta)
    //TODO explicit check after https://youtrack.jetbrains.com/issue/KT-32956
    @Suppress("UNCHECKED_CAST")
    return res as TaskResult<T>
}

public val TaskResultBuilder<*>.allData: DataSelector<*>
    get() = DataSelector { workspace, _ -> workspace.data }

/**
 * Perform a lazy mapping task using given [selector] and one-to-one [action].
 * TODO move selector to receiver with multi-receivers
 *
 * @param selector a workspace data selector. Could be either task selector or initial data selector.
 * @param dependencyMeta meta used for selector. The same meta is used for caching. By default, uses [defaultDependencyMeta].
 * @param dataMetaTransform additional transformation of individual data meta.
 * @param action process individual data asynchronously.
 */
@DFExperimental
public suspend inline fun <T, reified R> TaskResultBuilder<R>.transformEach(
    selector: DataSelector<T>,
    dependencyMeta: Meta = defaultDependencyMeta,
    dataMetaTransform: MutableMeta.(name: Name) -> Unit = {},
    crossinline action: suspend (arg: T, name: Name, meta: Meta) -> R,
) {
    from(selector, dependencyMeta).forEach { data ->
        val meta = data.meta.toMutableMeta().apply {
            taskMeta[taskName]?.let { taskName.put(it) }
            dataMetaTransform(data.name)
        }

        val res = data.transform(meta, workspace.context.coroutineContext) {
            action(it, data.name, meta)
        }

        data(data.name, res)
    }
}

/**
 * Set given [dataSet] as a task result.
 */
public fun <T> TaskResultBuilder<T>.result(dataSet: DataTree<T>) {
    branch(dataSet)
}

/**
 * Use provided [action] to fill the result
 */
@DFExperimental
public suspend inline fun <T, reified R> TaskResultBuilder<R>.actionFrom(
    selector: DataSelector<T>,
    action: Action<T, R>,
    dependencyMeta: Meta = defaultDependencyMeta,
) {
    branch(action.execute(workspace.context, from(selector, dependencyMeta), dependencyMeta))
}


