package space.kscience.dataforge.distributed

import io.lambdarpc.dsl.LibService
import kotlinx.serialization.KSerializer
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.distributed.ServiceWorkspace.Companion.execute
import space.kscience.dataforge.distributed.ServiceWorkspace.Companion.serviceId
import space.kscience.dataforge.distributed.serialization.DataSetPrototype
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.put
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.SerializableResultTask

/**
 * Workspace that exposes its tasks for remote clients.
 * @param port Port to start service on. Will be random if null.
 */
public class NodeWorkspace(
    port: Int? = null,
    context: Context = Global.buildContext("workspace".asName()),
    private val dataSerializer: KSerializer<Any>? = null,
    data: DataSet<*> = DataTree<Any>(),
    targets: Map<String, Meta> = mapOf(),
) : RemoteTaskWorkspace(context, data, targets), ServiceWorkspace {
    private val _port: Int? = port

    private val service = LibService(serviceId, port) {
        execute of { name, meta, executionContext ->
            if (name == Name.EMPTY) {
                requireNotNull(dataSerializer) { "Data serializer is not provided on $port" }
                DataSetPrototype.of(data, dataSerializer)
            } else {
                val proxyContext = context.buildContext(context.name + "proxy") {
                    properties {
                        put(executionContext)
                    }
                }
                val proxy = RemoteTaskWorkspace(context = proxyContext, data = data)
                val task = tasks[name] ?: error("Task with name $name not found in the workspace")
                require(task is SerializableResultTask)
                // Local function to capture generic parameter
                suspend fun <T : Any> execute(task: SerializableResultTask<T>): DataSetPrototype {
                    val result = task.execute(proxy, name, meta)
                    return DataSetPrototype.of(result, task.resultSerializer)
                }
                execute(task)
            }
        }
    }

    /**
     * Port this workspace is available on.
     */
    public override val port: Int
        get() = _port ?: service.port.p

    /**
     * Start [NodeWorkspace] as a service.
     */
    public override fun start(): Unit = service.start()

    /**
     * Await termination of the service.
     */
    public override fun awaitTermination(): Unit = service.awaitTermination()

    /**
     * Shutdown service.
     */
    public override fun shutdown(): Unit = service.shutdown()

    override fun close(): Unit = shutdown()
}
