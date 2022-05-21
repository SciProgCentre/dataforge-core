package space.kscience.dataforge.distributed.serialization

import io.lambdarpc.coding.Coder
import io.lambdarpc.coding.CodingContext
import io.lambdarpc.transport.grpc.Entity
import io.lambdarpc.transport.serialization.Entity
import io.lambdarpc.transport.serialization.RawData
import kotlinx.serialization.json.Json
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaSerializer
import java.nio.charset.Charset

internal object MetaCoder : Coder<Meta> {
    override suspend fun decode(entity: Entity, context: CodingContext): Meta {
        val string = entity.data.toString(Charset.defaultCharset())
        return Json.decodeFromString(MetaSerializer, string)
    }

    override suspend fun encode(value: Meta, context: CodingContext): Entity {
        val string = Json.encodeToString(MetaSerializer, value)
        return Entity(RawData.copyFrom(string, Charset.defaultCharset()))
    }
}
