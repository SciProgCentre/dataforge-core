package space.kscience.dataforge.workspace

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.asSequence
import space.kscience.dataforge.data.launchIn
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

/**
 * A result of a [Task]
 * @param workspace the [Workspace] that produced the result
 * @param taskName the name of the task that produced the result
 * @param taskMeta The configuration of the task that produced the result
 */
public data class TaskResult<T>(
    public val content: DataTree<T>,
    public val workspace: Workspace,
    public val taskName: Name,
    public val taskMeta: Meta,
) : DataTree<T> by content

/**
 * Wrap data into [TaskResult]
 */
public fun <T> Workspace.wrapResult(data: DataTree<T>, taskName: Name, taskMeta: Meta): TaskResult<T> =
    TaskResult(data, this, taskName, taskMeta)

/**
 * Start computation for all data elements of this node.
 * The resulting [Job] is completed only when all of them are completed.
 */
public fun TaskResult<*>.launchIn(scope: CoroutineScope): Job {
    val jobs = asSequence().map {
        it.launchIn(scope)
    }.toList()
    return scope.launch { jobs.joinAll() }
}