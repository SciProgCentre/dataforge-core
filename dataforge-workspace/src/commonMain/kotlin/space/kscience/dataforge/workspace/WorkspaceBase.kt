package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name


/**
 * A simple workspace without caching
 */
public class WorkspaceBase internal constructor(
    override val context: Context,
    data: DataSet<*>,
    override val targets: Map<String, Meta>,
    private val externalTasks: Map<Name, Task<*>>,
    private val postProcess: suspend (TaskResult<*>) -> TaskResult<*>,
) : Workspace {

    override val data: TaskResult<*> = wrapResult(data, Name.EMPTY, Meta.EMPTY)

    override val tasks: Map<Name, Task<*>> by lazy { context.gather<Task<*>>(Task.TYPE) + externalTasks }

    override suspend fun produce(taskName: Name, taskMeta: Meta): TaskResult<*> =
        postProcess(super.produce(taskName, taskMeta))
}