package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.context.members
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta


/**
 * A simple workspace without caching
 */
class SimpleWorkspace(
    override val context: Context,
    override val data: DataNode<Any>,
    override val targets: Map<String, Meta>,
    tasks: Collection<Task<Any>>
) : Workspace {
    override val tasks: Map<String, Task<*>> by lazy {
        (context.members<Task<*>>(Task.TYPE) + tasks).associate { it.name to it }
    }

    companion object {
        fun build(parent: Context = Global, block: SimpleWorkspaceBuilder.() -> Unit): SimpleWorkspace =
            SimpleWorkspaceBuilder(parent).apply(block).build()
    }
}