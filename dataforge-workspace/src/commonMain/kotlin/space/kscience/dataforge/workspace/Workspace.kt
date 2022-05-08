package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.provider.Provider


public interface DataSelector<T: Any>{
    public suspend fun select(workspace: Workspace, meta: Meta): DataSet<T>
}

@Type(Workspace.TYPE)
public interface Workspace : ContextAware, Provider {
    /**
     * The whole data node for current workspace
     */
    public val data: TaskResult<*>

    /**
     * All targets associated with the workspace
     */
    public val targets: Map<String, Meta>

    /**
     * All stages associated with the workspace
     */
    public val tasks: Map<Name, Task<*>>

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            "target", Meta.TYPE -> targets.mapKeys { Name.parse(it.key)}
            Task.TYPE -> tasks
            Data.TYPE -> data.traverse().associateBy { it.name }
            else -> emptyMap()
        }
    }

    public suspend fun produce(taskName: Name, taskMeta: Meta): TaskResult<*> {
        if (taskName == Name.EMPTY) return data
        val task = tasks[taskName] ?: error("Task with name $taskName not found in the workspace")
        return task.execute(this, taskName, taskMeta)
    }

    public suspend fun produceData(taskName: Name, taskMeta: Meta, name: Name): TaskData<*>? =
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
