package space.kscience.dataforge.distributed

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.values.string
import space.kscience.dataforge.workspace.SerializableResultTask
import space.kscience.dataforge.workspace.Task
import space.kscience.dataforge.workspace.TaskResult
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.wrapResult

/**
 * Workspace that returns [RemoteTask] if such task should be
 * executed remotely according to the execution context.
 */
public open class RemoteTaskWorkspace(
    final override val context: Context = Global.buildContext("workspace".asName()),
    data: DataSet<*> = DataTree<Any>(),
    override val targets: Map<String, Meta> = mapOf(),
) : Workspace {

    override val data: TaskResult<*> = wrapResult(data, Name.EMPTY, Meta.EMPTY)

    private val _tasks: Map<Name, Task<*>> = context.gather(Task.TYPE)

    override val tasks: Map<Name, Task<*>>
        get() = object : AbstractMap<Name, Task<*>>() {
            override val entries: Set<Map.Entry<Name, Task<*>>>
                get() = _tasks.entries

            override fun get(key: Name): Task<*>? {
                val executionContext = context.properties[EXECUTION_CONTEXT]
                val endpoint = executionContext?.get(ENDPOINTS)?.toMeta()?.get(key) ?: return _tasks[key]
                val string = endpoint.value?.string ?: error("Endpoint is expected to be a string")
                val local = _tasks[key] ?: error("No task with name $key")
                require(local is SerializableResultTask) { "Task $key result is not serializable" }
                return RemoteTask(string, local.resultType, local.resultSerializer, local.descriptor, executionContext)
            }
        }

    public companion object {
        internal val EXECUTION_CONTEXT = "execution".asName()
        internal val ENDPOINTS = "endpoints".asName()
    }
}

public fun MutableMeta.endpoints(block: EndpointsBuilder.() -> Unit) {
    RemoteTaskWorkspace.EXECUTION_CONTEXT put {
        RemoteTaskWorkspace.ENDPOINTS put EndpointsBuilder().apply(block).build()
    }
}

public class EndpointsBuilder {
    private val endpoints = mutableMapOf<Name, String>()

    public infix fun Name.on(endpoint: String) {
        endpoints[this] = endpoint
    }

    internal fun build(): Meta = Meta {
        endpoints.forEach { (name, endpoint) ->
            name put endpoint
        }
    }
}
