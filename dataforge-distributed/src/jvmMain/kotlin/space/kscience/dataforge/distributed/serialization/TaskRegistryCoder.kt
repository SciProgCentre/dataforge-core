package space.kscience.dataforge.distributed.serialization

import io.lambdarpc.coding.Coder
import io.lambdarpc.coding.CodingContext
import io.lambdarpc.transport.grpc.Entity
import io.lambdarpc.transport.serialization.Entity
import io.lambdarpc.transport.serialization.RawData
import kotlinx.serialization.json.Json
import space.kscience.dataforge.distributed.TaskRegistry
import java.nio.charset.Charset

internal object TaskRegistryCoder : Coder<TaskRegistry> {
    override suspend fun decode(entity: Entity, context: CodingContext): TaskRegistry {
        val string = entity.data.toString(Charset.defaultCharset())
        return Json.decodeFromString(TaskRegistry.serializer(), string)
    }

    override suspend fun encode(value: TaskRegistry, context: CodingContext): Entity {
        val string = Json.encodeToString(TaskRegistry.serializer(), value)
        return Entity(RawData.copyFrom(string, Charset.defaultCharset()))
    }
}
