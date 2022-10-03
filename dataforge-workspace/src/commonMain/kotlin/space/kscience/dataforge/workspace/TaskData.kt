package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.NamedData
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

/**
 * A [Workspace]-locked [NamedData], that serves as a computation model.
 */
public interface TaskData<out T : Any> : NamedData<T> {
    /**
     * The [Workspace] this data belongs to
     */
    public val workspace: Workspace

    /**
     * The name of the stage that produced this data. [Name.EMPTY] if the workspace intrinsic data is used.
     */
    public val taskName: Name

    /**
     * Stage configuration used to produce this data.
     */
    public val taskMeta: Meta

    /**
     * Dependencies that allow to compute transitive dependencies as well.
     */
//    override val dependencies: Collection<TaskData<*>>
}

private class TaskDataImpl<out T : Any>(
    override val workspace: Workspace,
    override val data: Data<T>,
    override val name: Name,
    override val taskName: Name,
    override val taskMeta: Meta,
) : TaskData<T>, Data<T> by data {
//    override val dependencies: Collection<TaskData<*>> = data.dependencies.map {
//        it as? TaskData<*> ?: error("TaskData can't depend on external data")
//    }
}

public fun <T : Any> Workspace.wrapData(data: Data<T>, name: Name, taskName: Name, stageMeta: Meta): TaskData<T> =
    TaskDataImpl(this, data, name, taskName, stageMeta)

