package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.context.content
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.toName


/**
 * A simple workspace without caching
 */
class SimpleWorkspace(
    override val context: Context,
    override val data: DataNode<Any>,
    override val targets: Map<String, Meta>,
    tasks: Collection<Task<Any>>
) : Workspace {

    override val tasks: Map<Name, Task<*>> by lazy {
        context.content<Task<*>>(Task.TYPE) + tasks.associateBy { it.name.toName() }
    }

    companion object {
        fun build(parent: Context = Global, block: SimpleWorkspaceBuilder.() -> Unit): SimpleWorkspace =
            SimpleWorkspaceBuilder(parent).apply(block).build()
    }
}