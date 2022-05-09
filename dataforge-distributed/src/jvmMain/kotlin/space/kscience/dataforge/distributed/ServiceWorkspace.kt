package space.kscience.dataforge.distributed

import io.ktor.utils.io.core.*
import io.lambdarpc.dsl.LibService
import io.lambdarpc.dsl.def
import io.lambdarpc.utils.Address
import io.lambdarpc.utils.Port
import io.lambdarpc.utils.toSid
import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.distributed.serialization.DataSetCoder
import space.kscience.dataforge.distributed.serialization.NameCoder
import space.kscience.dataforge.distributed.serialization.SerializableDataSetAdapter
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.Task
import space.kscience.dataforge.workspace.TaskResult
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.wrapResult

/**
 * Workspace that exposes its tasks for remote clients.
 */
public class ServiceWorkspace(
    address: String = "localhost",
    port: Int? = null,
    override val context: Context = Global.buildContext("workspace".asName()),
    data: DataSet<*> = runBlocking { DataTree<Any> {} },
    override val targets: Map<String, Meta> = mapOf(),
) : Workspace, Closeable {

    override val data: TaskResult<*> = wrapResult(data, Name.EMPTY, Meta.EMPTY)

    override val tasks: Map<Name, Task<*>>
        get() = context.gather(Task.TYPE)

    private val service = LibService(serviceId, address, port) {
        execute of { name ->
            val res = produce(name, Meta.EMPTY)
            SerializableDataSetAdapter(res)
        }
    }

    /**
     * Address this workspace is available on.
     */
    public val address: Address = Address(address)

    /**
     * Port this workspace is available on.
     */
    public val port: Port
        get() = service.port

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
        internal val serviceId = "d41b95b1-828b-4444-8ff0-6f9c92a79246".toSid()
        internal val execute by serviceId.def(NameCoder, DataSetCoder)
    }
}
