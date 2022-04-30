package space.kscience.dataforge.workspace.distributed

import io.lambdarpc.dsl.ServiceDispatcher
import io.lambdarpc.utils.Endpoint
import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Task
import space.kscience.dataforge.workspace.TaskResult
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.wrapResult
import kotlin.reflect.KType

/**
 * Proxy task that communicates with the corresponding remote task.
 */
internal class RemoteTask<T : Any>(
    endpoint: Endpoint,
    override val resultType: KType,
    override val descriptor: MetaDescriptor? = null,
) : Task<T> {
    private val dispatcher = ServiceDispatcher(ServiceWorkspace.serviceId to endpoint)

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(
        workspace: Workspace,
        taskName: Name,
        taskMeta: Meta,
    ): TaskResult<T> = withContext(dispatcher) {
        val dataset = ServiceWorkspace.execute(taskName) as LazyDecodableDataSet
        dataset.finishDecoding(resultType)
        workspace.wrapResult(dataset as DataSet<T>, taskName, taskMeta)
    }
}
