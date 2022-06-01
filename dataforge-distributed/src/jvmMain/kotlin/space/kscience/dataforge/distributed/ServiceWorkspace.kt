package space.kscience.dataforge.distributed

import io.lambdarpc.dsl.def
import io.lambdarpc.dsl.j
import io.lambdarpc.utils.ServiceId
import space.kscience.dataforge.distributed.serialization.DataSetPrototype
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Workspace
import java.io.Closeable

/**
 * [Workspace] that can expose its tasks to other workspaces as a service.
 */
public interface ServiceWorkspace : Workspace, Closeable {
    public val port: Int
    public fun start()
    public fun awaitTermination()
    public fun shutdown()

    override fun close() {
        shutdown()
    }

    public companion object {
        internal val serviceId = ServiceId("d41b95b1-828b-4444-8ff0-6f9c92a79246")
        internal val execute by serviceId.def(j<Name>(), j(MetaSerializer), j(MetaSerializer), j<DataSetPrototype>())
    }
}
