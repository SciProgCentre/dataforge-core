package space.kscience.dataforge.distributed

import io.lambdarpc.utils.Endpoint
import kotlinx.serialization.Serializable
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Task

@Serializable
internal class TaskRegistry(val tasks: Map<Name, Endpoint>)

internal fun TaskRegistry(tasks: Map<Name, Task<*>>): TaskRegistry {
    val remotes = tasks.filterValues { it is RemoteTask<*> }
    val endpoints = remotes.mapValues { (_, task) ->
        require(task is RemoteTask)
        task.endpoint
    }
    return TaskRegistry(endpoints)
}
