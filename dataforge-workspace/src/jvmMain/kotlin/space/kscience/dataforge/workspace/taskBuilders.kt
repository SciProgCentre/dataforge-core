package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.select
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

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
