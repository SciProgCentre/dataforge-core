package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.forEach
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

    override fun iterator(): Iterator<TaskData<T>>

    override fun get(name: Name): TaskData<T>?
}

private class TaskResultImpl<out T : Any>(
    override val workspace: Workspace,
    override val taskName: Name,
    override val taskMeta: Meta,
    val dataSet: DataSet<T>,
) : TaskResult<T>, DataSet<T> by dataSet {

    override fun iterator(): Iterator<TaskData<T>> = iterator {
        dataSet.forEach {
            yield(workspace.wrapData(it, it.name, taskName, taskMeta))
        }
    }

    override fun get(name: Name): TaskData<T>? = dataSet[name]?.let {
        workspace.wrapData(it, name, taskName, taskMeta)
    }
}

/**
 * Wrap data into [TaskResult]
 */
public fun <T : Any> Workspace.wrapResult(dataSet: DataSet<T>, taskName: Name, taskMeta: Meta): TaskResult<T> =
    TaskResultImpl(this, taskName, taskMeta, dataSet)