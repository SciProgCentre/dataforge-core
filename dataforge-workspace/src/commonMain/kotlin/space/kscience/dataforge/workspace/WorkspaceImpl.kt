package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.ObservableDataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name


internal class WorkspaceImpl internal constructor(
    override val context: Context,
    override val data: ObservableDataTree<*>,
    override val targets: Map<String, Meta>,
    tasks: Map<Name, Task<*>>,
    private val postProcess: suspend (TaskResult<*>) -> TaskResult<*>,
) : Workspace {

    override val tasks: Map<Name, Task<*>> by lazy { context.gather<Task<*>>(Task.TYPE) + tasks }

    override suspend fun produce(taskName: Name, taskMeta: Meta): TaskResult<*> {
        val task = tasks[taskName] ?: error("Task with name $taskName not found in the workspace")
        return postProcess(task.execute(this, taskName, taskMeta))
    }
}