package hep.dataforge.workspace

import hep.dataforge.context.ContextAware
import hep.dataforge.context.Named
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.Type


@Type(Workspace.TYPE)
interface Workspace : ContextAware, Named, Provider {
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
    val tasks: Map<String, Task<*>>

    override fun provideTop(target: String, name: Name): Any? {
        return when (target) {
            "target", Meta.TYPE -> targets[name.toString()]
            Task.TYPE -> tasks[name.toString()]
            Data.TYPE -> data[name]
            DataNode.TYPE -> data.getNode(name)
            else -> null
        }
    }

    override fun listTop(target: String): Sequence<Name> {
        return when (target) {
            "target", Meta.TYPE -> targets.keys.asSequence().map { it.toName() }
            Task.TYPE -> tasks.keys.asSequence().map { it.toName() }
            Data.TYPE -> data.dataSequence().map { it.first }
            DataNode.TYPE -> data.nodeSequence().map { it.first }
            else -> emptySequence()
        }
    }

    companion object {
        const val TYPE = "workspace"
    }
}

