package hep.dataforge.workspace

import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.builder
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.plus

/**
 * A dependency of the task which allows to lazily create a data tree for single dependency
 */
public sealed class Dependency : MetaRepr {
    public abstract suspend fun apply(workspace: Workspace): DataSet<Any>
}

public class DataDependency(private val placement: DataPlacement = DataPlacement.ALL) : Dependency() {
    override suspend fun apply(workspace: Workspace): DataSet<Any> = workspace.data.rearrange(placement)

    override fun toMeta(): Meta = placement.toMeta()
}

public abstract class TaskDependency<out T : Any>(
    public val meta: Meta,
    protected val placement: DataPlacement,
) : Dependency() {
    public abstract fun resolveTask(workspace: Workspace): Task<T>

    /**
     * A name of the dependency for logging and serialization
     */
    public abstract val name: Name

    override suspend fun apply(workspace: Workspace): DataSet<T> {
        val task = resolveTask(workspace)
        val result = workspace.run(task, meta)
        return result.rearrange(placement)
    }
}

public class ExternalTaskDependency<T : Any>(
    public val task: Task<T>,
    meta: Meta,
    placement: DataPlacement,
) : TaskDependency<T>(meta, placement) {
    override fun resolveTask(workspace: Workspace): Task<T> = task

    override val name: Name get() = EXTERNAL_TASK_NAME + task.name

    override fun toMeta(): Meta = placement.toMeta().builder().apply {
        "name" put name.toString()
        "task" put task.toString()
        "meta" put meta
    }

    public companion object {
        public val EXTERNAL_TASK_NAME: Name = "@external".asName()
    }
}

public class WorkspaceTaskDependency(
    override val name: Name,
    meta: Meta,
    placement: DataPlacement,
) : TaskDependency<Any>(meta, placement) {
    override fun resolveTask(workspace: Workspace): Task<*> = workspace.tasks[name]
        ?: error("Task with name $name is not found in the workspace")

    override fun toMeta(): Meta {
        TODO("Not yet implemented")
    }
}