package space.kscience.dataforge.workspace

import kotlinx.coroutines.CoroutineScope
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DfType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.provider.Provider
import kotlin.coroutines.CoroutineContext


public fun interface DataSelector<T> {
    public suspend fun select(workspace: Workspace, meta: Meta): DataTree<T>
}

/**
 * An environment for pull-mode computation
 */
@DfType(Workspace.TYPE)
public interface Workspace : ContextAware, Provider, CoroutineScope {

    override val coroutineContext: CoroutineContext get() = context.coroutineContext

    /**
     * The whole data node for current workspace
     */
    public val data: ObservableDataTree<*>

    /**
     * All targets associated with the workspace
     */
    public val targets: Map<String, Meta>

    /**
     * All tasks associated with the workspace
     */
    public val tasks: Map<Name, Task<*>>

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            "target", Meta.TYPE -> targets.mapKeys { Name.parse(it.key) }
            Task.TYPE -> tasks
            Data.TYPE -> data.asSequence().associateBy { it.name }
            else -> emptyMap()
        }
    }

    public suspend fun produce(taskName: Name, taskMeta: Meta): TaskResult<*> {
        val task = tasks[taskName] ?: error("Task with name $taskName not found in the workspace")
        return task.execute(this, taskName, taskMeta)
    }

    public suspend fun produceData(taskName: Name, taskMeta: Meta, name: Name): Data<*>? =
        produce(taskName, taskMeta)[name]

    public companion object {
        public const val TYPE: String = "workspace"
    }
}

public suspend fun Workspace.produce(task: String, target: String): TaskResult<*> =
    produce(Name.parse(task), targets[target] ?: error("Target with key $target not found in $this"))

public suspend fun Workspace.produce(task: String, meta: Meta): TaskResult<*> =
    produce(Name.parse(task), meta)

public suspend fun Workspace.produce(task: String, block: MutableMeta.() -> Unit = {}): TaskResult<*> =
    produce(task, Meta(block))
