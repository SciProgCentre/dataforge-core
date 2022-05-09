package space.kscience.dataforge.distributed.serialization

import io.lambdarpc.coding.Coder
import io.lambdarpc.coding.CodingContext
import io.lambdarpc.transport.grpc.Entity
import io.lambdarpc.transport.serialization.Entity
import io.lambdarpc.transport.serialization.RawData
import space.kscience.dataforge.names.Name
import java.nio.charset.Charset

internal object NameCoder : Coder<Name> {
    override fun decode(entity: Entity, context: CodingContext): Name {
        require(entity.hasData()) { "Entity should contain data" }
        val string = entity.data.toString(Charset.defaultCharset())
        return Name.parse(string)
    }

    override fun encode(value: Name, context: CodingContext): Entity =
        Entity(RawData.copyFrom(value.toString(), Charset.defaultCharset()))
}
