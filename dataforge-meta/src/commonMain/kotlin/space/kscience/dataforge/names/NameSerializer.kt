package space.kscience.dataforge.names

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object NameSerializer : KSerializer<Name> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("space.kscience.dataforge.names.Name", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Name {
        return Name.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Name) {
        encoder.encodeString(value.toString())
    }
}

public object NameTokenSerializer: KSerializer<NameToken> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("space.kscience.dataforge.names.NameToken", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NameToken {
        return Name.parse(decoder.decodeString()).firstOrNull()!!
    }

    override fun serialize(
        encoder: Encoder,
        value: NameToken,
    ) {
        encoder.encodeString(value.toString())
    }
}