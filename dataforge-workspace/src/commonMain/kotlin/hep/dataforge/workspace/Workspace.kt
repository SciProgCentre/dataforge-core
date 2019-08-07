package hep.dataforge.workspace

import hep.dataforge.context.ContextAware
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.Type


@Type(Workspace.TYPE)
interface Workspace : ContextAware, Provider {
    /**
     * The whole data node for current workspace
     */
    val data: DataNode<Any>

    /**
     * All targets associated with the workspace
     */
    val targets: Map<String, Meta>

    /**
     * All tasks associated with the workspace
     */
    val tasks: Map<Name, Task<*>>

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            "target", Meta.TYPE -> targets.mapKeys { it.key.toName() }
            Task.TYPE -> tasks
            Data.TYPE -> data.data.toMap()
            DataNode.TYPE -> data.nodes.toMap()
            else -> emptyMap()
        }
    }

    /**
     * Invoke a task in the workspace utilizing caching if possible
     */
    fun <R : Any> run(task: Task<R>, config: Meta): DataNode<R> {
        context.activate(this)
        try {
            val model = task.build(this, config)
            task.validate(model)
            return task.run(this, model)
        } finally {
            context.deactivate(this)
        }
    }

    companion object {
        const val TYPE = "workspace"
    }
}

fun Workspace.run(task: Task<*>, target: String): DataNode<Any> {
    val meta = targets[target] ?: error("A target with name $target not found in ${this}")
    return run(task, meta)
}


fun Workspace.run(task: String, target: String) =
    tasks[task.toName()]?.let { run(it, target) } ?: error("Task with name $task not found")

fun Workspace.run(task: String, meta: Meta) =
    tasks[task.toName()]?.let { run(it, meta) } ?: error("Task with name $task not found")

fun Workspace.run(task: String, block: MetaBuilder.() -> Unit = {}) =
    run(task, buildMeta(block))
