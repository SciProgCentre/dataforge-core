package hep.dataforge.workspace

import hep.dataforge.data.DataFilter
import hep.dataforge.data.DataNode
import hep.dataforge.data.filter
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.isEmpty
import hep.dataforge.names.plus

/**
 * A dependency of the task which allows to lazily create a data tree for single dependency
 */
sealed class Dependency : MetaRepr {
    abstract fun apply(workspace: Workspace): DataNode<Any>
}

class DataDependency(val filter: DataFilter, val placement: Name = Name.EMPTY) : Dependency() {
    override fun apply(workspace: Workspace): DataNode<Any> {
        val result = workspace.data.filter(filter)
        return if (placement.isEmpty()) {
            result
        } else {
            DataNode.invoke(Any::class) { this[placement] = result }
        }
    }

    override fun toMeta(): Meta = buildMeta {
        "data" put filter.config
        "to" put placement.toString()
    }
}

class AllDataDependency(val placement: Name = Name.EMPTY) : Dependency() {
    override fun apply(workspace: Workspace): DataNode<Any> = if (placement.isEmpty()) {
        workspace.data
    } else {
        DataNode.invoke(Any::class) { this[placement] = workspace.data }
    }

    override fun toMeta() = buildMeta {
        "data" put "@all"
        "to" put placement.toString()
    }
}

abstract class TaskDependency<out T : Any>(
    val meta: Meta,
    val placement: Name = Name.EMPTY
) : Dependency() {
    abstract fun resolveTask(workspace: Workspace): Task<T>

    /**
     * A name of the dependency for logging and serialization
     */
    abstract val name: Name

    override fun apply(workspace: Workspace): DataNode<T> {
        val task = resolveTask(workspace)
        if (task.isTerminal) TODO("Support terminal task")
        val result = workspace.run(task, meta)
        return if (placement.isEmpty()) {
            result
        } else {
            DataNode(task.type) { this[placement] = result }
        }
    }

    override fun toMeta(): Meta = buildMeta {
        "task" put name.toString()
        "meta" put meta
        "to" put placement.toString()
    }
}

class DirectTaskDependency<T : Any>(
    val task: Task<T>,
    meta: Meta,
    placement: Name
) : TaskDependency<T>(meta, placement) {
    override fun resolveTask(workspace: Workspace): Task<T> = task

    override val name: Name get() = DIRECT_TASK_NAME + task.name

    companion object {
        val DIRECT_TASK_NAME = "@direct".asName()
    }
}

class WorkspaceTaskDependency(
    override val name: Name,
    meta: Meta,
    placement: Name
) : TaskDependency<Any>(meta, placement) {
    override fun resolveTask(workspace: Workspace): Task<*> =
        workspace.tasks[name] ?: error("Task with name $name is not found in the workspace")
}