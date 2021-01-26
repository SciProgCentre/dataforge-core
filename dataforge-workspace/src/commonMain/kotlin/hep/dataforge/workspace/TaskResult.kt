package hep.dataforge.workspace

import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    override fun flow(): Flow<TaskData<T>>
    override suspend fun getData(name: Name): TaskData<T>?
}

private class TaskResultImpl<out T : Any>(
    override val workspace: Workspace,
    val dataSet: DataSet<T>,
    override val taskName: Name,
    override val taskMeta: Meta,
) : TaskResult<T>, DataSet<T> by dataSet {

    override fun flow(): Flow<TaskData<T>> = dataSet.flow().map {
        workspace.internalize(it, it.name, taskName, taskMeta)
    }

    override suspend fun getData(name: Name): TaskData<T>? = dataSet.getData(name)?.let {
        workspace.internalize(it, name, taskName, taskMeta)
    }
}

internal fun <T : Any> Workspace.internalize(dataSet: DataSet<T>, stage: Name, stageMeta: Meta): TaskResult<T> =
    TaskResultImpl(this, dataSet, stage, stageMeta)