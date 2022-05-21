package space.kscience.dataforge.distributed

import io.lambdarpc.utils.Endpoint
import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.Plugin
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.SerializableResultTask
import space.kscience.dataforge.workspace.Task

/**
 * Plugin that purpose is to communicate with remote plugins.
 * @param plugin A remote plugin to be used.
 * @param endpoint Endpoint of the remote plugin.
 */
public class RemotePlugin<P : Plugin>(private val plugin: P, private val endpoint: String) : AbstractPlugin() {

    public constructor(factory: PluginFactory<P>, endpoint: String) : this(factory(), endpoint)

    override val tag: PluginTag
        get() = plugin.tag

    private val tasks = plugin.content(Task.TYPE)
        .filterValues { it is SerializableResultTask<*> }
        .mapValues { (_, task) ->
            require(task is SerializableResultTask<*>)
            RemoteTask(Endpoint(endpoint), task.resultType, task.resultSerializer)
        }

    override fun content(target: String): Map<Name, Any> =
        when (target) {
            Task.TYPE -> tasks
            else -> emptyMap()
        }
}
