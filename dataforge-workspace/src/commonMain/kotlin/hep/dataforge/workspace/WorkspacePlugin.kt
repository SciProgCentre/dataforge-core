package hep.dataforge.workspace

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * An abstract plugin with some additional boilerplate to effectively work with workspace context
 */
abstract class WorkspacePlugin : AbstractPlugin() {
    abstract val tasks: Collection<Task<*>>

    override fun provideTop(target: String, name: Name): Any? {
        return if (target == Task.TYPE) {
            tasks.find { it.name == name.toString() }
        } else {
            super.provideTop(target, name)
        }
    }

    override fun listNames(target: String): Sequence<Name> {
        return if (target == Task.TYPE) {
            tasks.asSequence().map { it.name.toName() }
        } else {
            return super.listNames(target)
        }
    }
}