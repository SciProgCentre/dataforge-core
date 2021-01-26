package hep.dataforge.workspace

import hep.dataforge.data.DataSet
import hep.dataforge.data.select
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name

public suspend inline fun <reified T : Any> TaskResultBuilder<T>.from(
    task: Name,
    taskMeta: Meta = Meta.EMPTY,
): DataSet<T> = workspace.produce(task, taskMeta).select()


@Suppress("UNCHECKED_CAST")
public suspend fun <R : Any> TaskResultBuilder<*>.from(
    reference: TaskReference<R>,
    taskMeta: Meta = Meta.EMPTY,
): DataSet<R> {
    if (workspace.tasks[reference.taskName] == reference.task) {
        return workspace.produce(reference.taskName, taskMeta) as TaskResult<R>
    } else {
        throw error("Task ${reference.taskName} does not belong to the workspace")
    }
}
