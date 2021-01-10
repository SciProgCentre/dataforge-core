package hep.dataforge.workspace

import hep.dataforge.context.ContextAware
import hep.dataforge.data.DataSet
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
    public val data: DataSet<Any>

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
            "target", Meta.TYPE -> targets.mapKeys { it.key.toName() }
            Task.TYPE -> tasks
            //Data.TYPE -> data.flow().toMap()
            else -> emptyMap()
        }
    }

    /**
     * Invoke a task in the workspace utilizing caching if possible
     */
    public suspend fun <R : Any> run(task: Task<R>, config: Meta): DataSet<R> {
        val model = task.build(this, config)
        task.validate(model)
        return task.run(this, model)
    }

    public companion object {
        public const val TYPE: String = "workspace"
    }
}

public suspend fun Workspace.run(task: Task<*>, target: String): DataSet<Any> {
    val meta = targets[target] ?: error("A target with name $target not found in $this")
    return run(task, meta)
}


public suspend fun Workspace.run(task: String, target: String): DataSet<Any> =
    tasks[task.toName()]?.let { run(it, target) } ?: error("Task with name $task not found")

public suspend fun Workspace.run(task: String, meta: Meta): DataSet<Any> =
    tasks[task.toName()]?.let { run(it, meta) } ?: error("Task with name $task not found")

public suspend fun Workspace.run(task: String, block: MetaBuilder.() -> Unit = {}): DataSet<Any> =
    run(task, Meta(block))

public suspend fun <T : Any> Workspace.run(task: Task<T>, metaBuilder: MetaBuilder.() -> Unit = {}): DataSet<T> =
    run(task, Meta(metaBuilder))