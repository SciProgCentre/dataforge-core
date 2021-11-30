package space.kscience.dataforge.workspace

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

/**
 * A result of a [Task]
 */
public interface TaskResult<out T : Any> : DataSet<T> {
    /**
     * The [Workspace] this [DataSet] belongs to
     */
    public val workspace: Workspace

    /**
     * The [Name] of the stage that produced this [DataSet]
     */
    public val taskName: Name

    /**
     * The configuration of the stage that produced this [DataSet]
     */
    public val taskMeta: Meta

    override fun flowData(): Flow<TaskData<T>>
    override suspend fun getData(name: Name): TaskData<T>?
}

private class TaskResultImpl<out T : Any>(
    override val workspace: Workspace,
    val dataSet: DataSet<T>,
    override val taskName: Name,
    override val taskMeta: Meta,
) : TaskResult<T>, DataSet<T> by dataSet {

    override fun flowData(): Flow<TaskData<T>> = dataSet.flowData().map {
        workspace.wrapData(it, it.name, taskName, taskMeta)
    }

    override suspend fun getData(name: Name): TaskData<T>? = dataSet.getData(name)?.let {
        workspace.wrapData(it, name, taskName, taskMeta)
    }
}

/**
 * Wrap data into [TaskResult]
 */
public fun <T : Any> Workspace.wrapResult(dataSet: DataSet<T>, taskName: Name, taskMeta: Meta): TaskResult<T> =
    TaskResultImpl(this, dataSet, taskName, taskMeta)