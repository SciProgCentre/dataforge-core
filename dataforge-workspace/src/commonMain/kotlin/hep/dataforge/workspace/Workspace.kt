package hep.dataforge.workspace

import hep.dataforge.context.ContextAware
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.misc.Type
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider


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
            "target", Meta.TYPE -> targets.mapKeys { it.key.toName() }
            Task.TYPE -> tasks
            //Data.TYPE -> data.flow().toMap()
            else -> emptyMap()
        }
    }

    public suspend fun produce(taskName: Name, taskMeta: Meta): TaskResult<*> {
        if (taskName == Name.EMPTY) return data
        val task = tasks[taskName] ?: error("Task with name $taskName not found in the workspace")
        return task.execute(this, taskName, taskMeta)
    }

    public suspend fun produceData(taskName: Name, taskMeta: Meta, name: Name): TaskData<*>? =
        produce(taskName, taskMeta).getData(name)

    public companion object {
        public const val TYPE: String = "workspace"
    }
}

public suspend fun Workspace.produce(task: String, target: String): TaskResult<*> =
    produce(task.toName(), targets[target] ?: error("Target with key $target not found in $this"))

public suspend fun Workspace.produce(task: String, meta: Meta): TaskResult<*> =
    produce(task.toName(), meta)

public suspend fun Workspace.produce(task: String, block: MetaBuilder.() -> Unit = {}): TaskResult<*> =
    produce(task, Meta(block))
