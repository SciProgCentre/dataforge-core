package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.context.Global
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.data.dataSequence
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.Type


@Type(Workspace.TYPE)
public interface Workspace : ContextAware, Provider {
    /**
     * The whole data node for current workspace
     */
    public val data: DataNode<Any>

    /**
     * All targets associated with the workspace
     */
    public val targets: Map<String, Meta>

    /**
     * All tasks associated with the workspace
     */
    public val tasks: Map<Name, Task<*>>

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            "target", Meta.TYPE -> targets.mapKeys { it.key.toName() }
            Task.TYPE -> tasks
            Data.TYPE -> data.dataSequence().toMap()
            //DataNode.TYPE -> data.nodes.toMap()
            else -> emptyMap()
        }
    }

    /**
     * Invoke a task in the workspace utilizing caching if possible
     */
    public fun <R : Any> run(task: Task<R>, config: Meta): DataNode<R> {
        context.activate(this)
        try {
            val model = task.build(this, config)
            task.validate(model)
            return task.run(this, model)
        } finally {
            context.deactivate(this)
        }
    }

    public companion object {
        public const val TYPE: String = "workspace"
        public operator fun invoke(parent: Context = Global, block: SimpleWorkspaceBuilder.() -> Unit): SimpleWorkspace =
            SimpleWorkspaceBuilder(parent).apply(block).build()
    }
}

public fun Workspace.run(task: Task<*>, target: String): DataNode<Any> {
    val meta = targets[target] ?: error("A target with name $target not found in ${this}")
    return run(task, meta)
}


public fun Workspace.run(task: String, target: String): DataNode<Any> =
    tasks[task.toName()]?.let { run(it, target) } ?: error("Task with name $task not found")

public fun Workspace.run(task: String, meta: Meta): DataNode<Any> =
    tasks[task.toName()]?.let { run(it, meta) } ?: error("Task with name $task not found")

public fun Workspace.run(task: String, block: MetaBuilder.() -> Unit = {}): DataNode<Any> =
    run(task, Meta(block))

public fun <T: Any> Workspace.run(task: Task<T>, metaBuilder: MetaBuilder.() -> Unit = {}): DataNode<T> =
    run(task, Meta(metaBuilder))