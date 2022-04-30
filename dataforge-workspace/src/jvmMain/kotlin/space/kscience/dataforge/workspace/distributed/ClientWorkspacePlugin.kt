package space.kscience.dataforge.workspace.distributed

import io.lambdarpc.utils.Endpoint
import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Task
import kotlin.reflect.KType

/**
 * Plugin that purpose is to communicate with remote plugins.
 * @param tag Tag og the [ClientWorkspacePlugin] should be equal to the tag of the corresponding remote plugin.
 * @param endpoint Endpoint of the remote plugin.
 * @param tasks Enumeration of names of remote tasks and their result types.
 */
public abstract class ClientWorkspacePlugin(
    override val tag: PluginTag,
    endpoint: Endpoint,
    vararg tasks: Pair<Name, KType>,
) : AbstractPlugin() {

    private val tasks: Map<Name, Task<*>> = tasks.associate { (name, type) ->
        name to RemoteTask<Any>(endpoint, type)
    }

    override fun content(target: String): Map<Name, Any> =
        when (target) {
            Task.TYPE -> tasks
            else -> emptyMap()
        }
}
