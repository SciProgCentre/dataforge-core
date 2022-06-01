package space.kscience.dataforge.distributed

import io.lambdarpc.context.ServiceDispatcher
import io.lambdarpc.utils.Endpoint
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.SerializableResultTask
import space.kscience.dataforge.workspace.TaskResult
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.wrapResult
import kotlin.reflect.KType

/**
 * Proxy task that communicates with the corresponding remote task.
 */
internal class RemoteTask<T : Any>(
    endpoint: String,
    override val resultType: KType,
    override val resultSerializer: KSerializer<T>,
    override val descriptor: MetaDescriptor? = null,
    private val executionContext: Meta = Meta.EMPTY,
) : SerializableResultTask<T> {
    private val dispatcher = ServiceDispatcher(ServiceWorkspace.serviceId to Endpoint(endpoint))

    override suspend fun execute(workspace: Workspace, taskName: Name, taskMeta: Meta): TaskResult<T> {
        val result = withContext(dispatcher) {
            ServiceWorkspace.execute(taskName, taskMeta, executionContext)
        }
        val dataSet = result.toDataSet(resultType, resultSerializer)
        return workspace.wrapResult(dataSet, taskName, taskMeta)
    }
}
