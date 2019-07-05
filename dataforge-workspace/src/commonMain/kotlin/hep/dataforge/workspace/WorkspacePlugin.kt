package hep.dataforge.workspace

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * An abstract plugin with some additional boilerplate to effectively work with workspace context
 */
abstract class WorkspacePlugin : AbstractPlugin() {
    abstract val tasks: Collection<Task<*>>

    override fun provideTop(target: String): Map<Name, Any> {
        return when(target){
            Task.TYPE -> tasks.associateBy { it.name.toName() }
            else -> emptyMap()
        }
    }
}