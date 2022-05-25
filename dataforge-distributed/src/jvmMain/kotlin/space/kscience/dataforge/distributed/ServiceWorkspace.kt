package space.kscience.dataforge.distributed

import io.ktor.utils.io.core.*
import io.lambdarpc.coding.coders.JsonCoder
import io.lambdarpc.dsl.LibService
import io.lambdarpc.dsl.def
import io.lambdarpc.dsl.j
import io.lambdarpc.utils.ServiceId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.distributed.serialization.DataSetPrototype
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.SerializableResultTask
import space.kscience.dataforge.workspace.Task
import space.kscience.dataforge.workspace.TaskResult
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.wrapResult

/**
 * Workspace that exposes its tasks for remote clients.
 * @param port Port to start service on. Will be random if null.
 */
public class ServiceWorkspace(
    port: Int? = null,
    override val context: Context = Global.buildContext("workspace".asName()),
    private val dataSerializer: KSerializer<Any>? = null,
    data: DataSet<*> = runBlocking { DataTree<Any> {} },
    override val targets: Map<String, Meta> = mapOf(),
) : Workspace, Closeable {
    private val _port: Int? = port

    override val data: TaskResult<*> = wrapResult(data, Name.EMPTY, Meta.EMPTY)

    override val tasks: Map<Name, Task<*>>
        get() = context.gather(Task.TYPE)

    private val service = LibService(serviceId, port) {
        execute of { name, meta, taskRegistry ->
            if (name == Name.EMPTY) {
                requireNotNull(dataSerializer) { "Data serializer is not provided on $port" }
                DataSetPrototype.of(data, dataSerializer)
            } else {
                val task = tasks[name] ?: error("Task $name does not exist locally")
                require(task is SerializableResultTask) { "Result of $name cannot be serialized" }
                val workspace = ProxyWorkspace(taskRegistry)

                // Local function to capture generic parameter
                suspend fun <T : Any> execute(task: SerializableResultTask<T>): DataSetPrototype {
                    val result = task.execute(workspace, name, meta)
                    return DataSetPrototype.of(result, task.resultSerializer)
                }
                execute(task)
            }
        }
    }

    /**
     * Proxies task calls to right endpoints according to the [TaskRegistry].
     */
    private inner class ProxyWorkspace(private val taskRegistry: TaskRegistry) : Workspace by this {
        override val tasks: Map<Name, Task<*>>
            get() = object : AbstractMap<Name, Task<*>>() {
                override val entries: Set<Map.Entry<Name, Task<*>>>
                    get() = this@ServiceWorkspace.tasks.entries

                override fun get(key: Name): Task<*>? = remoteTask(key) ?: this@ServiceWorkspace.tasks[key]
            }

        /**
         * Call default implementation to use [tasks] virtually instead of it in [ServiceWorkspace].
         */
        override suspend fun produce(taskName: Name, taskMeta: Meta): TaskResult<*> =
            super.produce(taskName, taskMeta)

        private fun remoteTask(name: Name): RemoteTask<*>? {
            val endpoint = taskRegistry.tasks[name] ?: return null
            val local = this@ServiceWorkspace.tasks[name] ?: error("No task with name $name locally on $port")
            require(local is SerializableResultTask) { "Task $name result is not serializable" }
            return RemoteTask(endpoint, local.resultType, local.resultSerializer, local.descriptor, taskRegistry)
        }
    }

    /**
     * Port this workspace is available on.
     */
    public val port: Int
        get() = _port ?: service.port.p

    /**
     * Start [ServiceWorkspace] as a service.
     */
    public fun start(): Unit = service.start()

    /**
     * Await termination of the service.
     */
    public fun awaitTermination(): Unit = service.awaitTermination()

    /**
     * Shutdown service.
     */
    public fun shutdown(): Unit = service.shutdown()

    override fun close(): Unit = service.shutdown()

    public companion object {
        internal val serviceId = ServiceId("d41b95b1-828b-4444-8ff0-6f9c92a79246")
        internal val execute by serviceId.def(
            JsonCoder(Name.serializer()), JsonCoder(MetaSerializer), j<TaskRegistry>(),
            j<DataSetPrototype>()
        )
    }
}
