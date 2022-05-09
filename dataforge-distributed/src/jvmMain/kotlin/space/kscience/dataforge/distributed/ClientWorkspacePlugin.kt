package space.kscience.dataforge.distributed

import io.lambdarpc.utils.Endpoint
import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Task
import kotlin.reflect.KType

/**
 * Plugin that purpose is to communicate with remote plugins.
 * @param endpoint Endpoint of the remote plugin.
 */
public abstract class ClientWorkspacePlugin(endpoint: Endpoint) : AbstractPlugin() {

    /**
     * Tag og the [ClientWorkspacePlugin] should be equal to the tag of the corresponding remote plugin.
     */
    abstract override val tag: PluginTag

    /**
     * Enumeration of names of remote tasks and their result types.
     */
    public abstract val tasks: Map<Name, KType>

    private val _tasks: Map<Name, Task<*>> by lazy {
        tasks.mapValues { (_, type) ->
            RemoteTask<Any>(endpoint, type)
        }
    }

    override fun content(target: String): Map<Name, Any> =
        when (target) {
            Task.TYPE -> _tasks
            else -> emptyMap()
        }
}
