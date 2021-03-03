package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.names.Name

/**
 * An abstract plugin with some additional boilerplate to effectively work with workspace context
 */
public abstract class WorkspacePlugin : AbstractPlugin(), TaskContainer {
    private val tasks = HashMap<Name, Task<*>>()

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            Task.TYPE -> tasks
            else -> emptyMap()
        }
    }

    override fun registerTask(taskName: Name, task: Task<*>) {
        tasks[taskName] = task
    }
}