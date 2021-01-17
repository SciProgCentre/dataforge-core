package hep.dataforge.workspace

import hep.dataforge.context.ContextAware
import hep.dataforge.meta.Meta
import hep.dataforge.misc.Type
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider


@Type(Workspace.TYPE)
public interface Workspace : ContextAware, Provider {
    /**
     * The whole data node for current workspace
     */
    public val data: StageDataSet<*>

    /**
     * All targets associated with the workspace
     */
    public val targets: Map<String, Meta>

    /**
     * All stages associated with the workspace
     */
    public val stages: Map<Name, WorkStage<*>>

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            "target", Meta.TYPE -> targets.mapKeys { it.key.toName() }
            WorkStage.TYPE -> stages
            //Data.TYPE -> data.flow().toMap()
            else -> emptyMap()
        }
    }

    public companion object {
        public const val TYPE: String = "workspace"
    }
}

public suspend fun Workspace.stage(taskName: Name, taskMeta: Meta): StageDataSet<*> {
    val task = stages[taskName] ?: error("Task with name $taskName not found in the workspace")
    return task.execute(this, taskMeta)
}

public suspend fun Workspace.getData(taskName: Name, taskMeta: Meta, name: Name): StageData<*>? =
    stage(taskName, taskMeta).getData(name)

//public suspend fun Workspace.execute(task: WorkStage<*>, target: String): DataSet<Any> {
//    val meta = targets[target] ?: error("A target with name $target not found in $this")
//    return run(task, meta)
//}
//
//
//public suspend fun Workspace.execute(task: String, target: String): DataSet<Any> =
//    stages[task.toName()]?.let { execute(it, target) } ?: error("Task with name $task not found")
//
//public suspend fun Workspace.execute(task: String, meta: Meta): DataSet<Any> =
//    stages[task.toName()]?.let { run(it, meta) } ?: error("Task with name $task not found")
//
//public suspend fun Workspace.execute(task: String, block: MetaBuilder.() -> Unit = {}): DataSet<Any> =
//    execute(task, Meta(block))
//
//public suspend fun <T : Any> Workspace.execute(task: WorkStage<T>, metaBuilder: MetaBuilder.() -> Unit = {}): DataSet<T> =
//    run(task, Meta(metaBuilder))