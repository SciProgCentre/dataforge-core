package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.ObservableDataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType

/**
 * A result of a [Task]
 * @param workspace the [Workspace] that produced the result
 * @param taskName the name of the task that produced the result
 * @param taskMeta The configuration of the task that produced the result
 */
public data class TaskResult<T>(
    public val data: ObservableDataTree<T>,
    public val workspace: Workspace,
    public val taskName: Name,
    public val taskMeta: Meta,
) {
    val dataType: KType get() = data.dataType
}

/**
 * Wrap data into [TaskResult]
 */
public fun <T> Workspace.wrapResult(data: ObservableDataTree<T>, taskName: Name, taskMeta: Meta): TaskResult<T> =
    TaskResult(data, this, taskName, taskMeta)