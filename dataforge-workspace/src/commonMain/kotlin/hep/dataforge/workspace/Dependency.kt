package hep.dataforge.workspace

import hep.dataforge.data.DataFilter
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTree
import hep.dataforge.data.filter
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaRepr
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.isEmpty
import hep.dataforge.names.plus

/**
 * A dependency of the task which allows to lazily create a data tree for single dependency
 */
public sealed class Dependency : MetaRepr {
    public abstract fun apply(workspace: Workspace): DataNode<Any>
}

public class DataDependency(private val filter: DataFilter, private val placement: Name = Name.EMPTY) : Dependency() {
    override fun apply(workspace: Workspace): DataNode<Any> {
        val result = workspace.data.filter(filter)
        return if (placement.isEmpty()) {
            result
        } else {
            DataTree(Any::class) { this[placement] = result }
        }
    }

    override fun toMeta(): Meta = Meta {
        "data" put filter.toMeta()
        "to" put placement.toString()
    }
}

public class AllDataDependency(private val placement: Name = Name.EMPTY) : Dependency() {
    override fun apply(workspace: Workspace): DataNode<Any> = if (placement.isEmpty()) {
        workspace.data
    } else {
        DataTree(Any::class) { this[placement] = workspace.data }
    }

    override fun toMeta(): MetaBuilder = Meta {
        "data" put "@all"
        "to" put placement.toString()
    }
}

public abstract class TaskDependency<out T : Any>(
    public val meta: Meta,
    public val placement: Name = Name.EMPTY
) : Dependency() {
    public abstract fun resolveTask(workspace: Workspace): Task<T>

    /**
     * A name of the dependency for logging and serialization
     */
    public abstract val name: Name

    override fun apply(workspace: Workspace): DataNode<T> {
        val task = resolveTask(workspace)
        if (task.isTerminal) TODO("Support terminal task")
        val result = workspace.run(task, meta)
        return if (placement.isEmpty()) {
            result
        } else {
            DataTree(task.type) { this[placement] = result }
        }
    }

    override fun toMeta(): Meta = Meta {
        "task" put name.toString()
        "meta" put meta
        "to" put placement.toString()
    }
}

public class DirectTaskDependency<T : Any>(
    public val task: Task<T>,
    meta: Meta,
    placement: Name
) : TaskDependency<T>(meta, placement) {
    override fun resolveTask(workspace: Workspace): Task<T> = task

    override val name: Name get() = DIRECT_TASK_NAME + task.name

    public companion object {
        public val DIRECT_TASK_NAME: Name = "@direct".asName()
    }
}

public class WorkspaceTaskDependency(
    override val name: Name,
    meta: Meta,
    placement: Name
) : TaskDependency<Any>(meta, placement) {
    override fun resolveTask(workspace: Workspace): Task<*> =
        workspace.tasks[name] ?: error("Task with name $name is not found in the workspace")
}