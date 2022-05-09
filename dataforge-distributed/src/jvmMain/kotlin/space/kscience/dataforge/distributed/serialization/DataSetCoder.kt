package space.kscience.dataforge.distributed.serialization

import io.lambdarpc.coding.Coder
import io.lambdarpc.coding.CodingContext
import io.lambdarpc.transport.grpc.Entity
import io.lambdarpc.transport.serialization.Entity
import io.lambdarpc.transport.serialization.RawData
import java.nio.charset.Charset

internal object DataSetCoder : Coder<SerializableDataSet<Any>> {
    override fun decode(entity: Entity, context: CodingContext): SerializableDataSet<Any> {
        val string = entity.data.toString(Charset.defaultCharset())
        val prototype = DataSetPrototype.fromJson(string)
        return prototype.toDataSet()
    }

    override fun encode(value: SerializableDataSet<Any>, context: CodingContext): Entity {
        val prototype = DataSetPrototype.of(value)
        val string = prototype.toJson()
        return Entity(RawData.copyFrom(string, Charset.defaultCharset()))
    }
}
