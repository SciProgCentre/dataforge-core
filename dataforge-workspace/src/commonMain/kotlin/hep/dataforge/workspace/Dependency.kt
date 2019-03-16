package hep.dataforge.workspace

import hep.dataforge.data.DataFilter
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTreeBuilder
import hep.dataforge.data.filter
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.EmptyName
import hep.dataforge.names.Name
import hep.dataforge.names.isEmpty

/**
 * A dependency of the task which allows to lazily create a data tree for single dependency
 */
sealed class Dependency : MetaRepr {
    abstract fun apply(workspace: Workspace): DataNode<Any>
}

class DataDependency(val filter: DataFilter) : Dependency() {
    override fun apply(workspace: Workspace): DataNode<Any> =
        workspace.data.filter(filter)

    override fun toMeta(): Meta = filter.config

    companion object {
        val all: DataDependency = DataDependency(DataFilter.build { })
    }
}

class TaskModelDependency(val name: String, val meta: Meta, val placement: Name = EmptyName) : Dependency() {
    override fun apply(workspace: Workspace): DataNode<Any> {
        val task = workspace.tasks[name] ?: error("Task with name ${name} is not found in the workspace")
        if (task.isTerminal) TODO("Support terminal task")
        val result = with(workspace) { task(meta) }
        return if (placement.isEmpty()) {
            result
        } else {
            DataTreeBuilder<Any>().apply { this[placement] = result }.build()
        }
    }

    override fun toMeta(): Meta = buildMeta {
        "name" to name
        "meta" to meta
    }
}