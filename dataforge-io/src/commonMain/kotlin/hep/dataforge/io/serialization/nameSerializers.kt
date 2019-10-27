package hep.dataforge.io.serialization

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(Name::class)
object NameSerializer : KSerializer<Name> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("Name")

    override fun deserialize(decoder: Decoder): Name {
        return decoder.decodeString().toName()
    }

    override fun serialize(encoder: Encoder, obj: Name) {
        encoder.encodeString(obj.toString())
    }
}

@Serializer(NameToken::class)
object NameTokenSerializer : KSerializer<NameToken> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("NameToken")

    override fun deserialize(decoder: Decoder): NameToken {
        return decoder.decodeString().toName().first()!!
    }

    override fun serialize(encoder: Encoder, obj: NameToken) {
        encoder.encodeString(obj.toString())
    }
}