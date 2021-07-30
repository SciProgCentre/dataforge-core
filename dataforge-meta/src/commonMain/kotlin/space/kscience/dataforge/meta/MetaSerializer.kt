package space.kscience.dataforge.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

/**
 * Serialized for [Meta]
 */
public object MetaSerializer : KSerializer<Meta> {
    private val genericMetaSerializer = SealedMeta.serializer()

    private val jsonSerializer = JsonElement.serializer()

    override val descriptor: SerialDescriptor = jsonSerializer.descriptor

    override fun deserialize(decoder: Decoder): Meta = if (decoder is JsonDecoder) {
        jsonSerializer.deserialize(decoder).toMeta()
    } else {
        genericMetaSerializer.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: Meta) {
        if (encoder is JsonEncoder) {
            jsonSerializer.serialize(encoder, value.toJson())
        } else {
            genericMetaSerializer.serialize(encoder, value.seal())
        }
    }
}

/**
 * A serializer for [MutableMeta]
 */
public object MutableMetaSerializer : KSerializer<MutableMeta> {

    override val descriptor: SerialDescriptor = MetaSerializer.descriptor

    override fun deserialize(decoder: Decoder): MutableMeta {
        val meta = decoder.decodeSerializableValue(MetaSerializer)
        return (meta as? MutableMeta) ?: meta.toMutableMeta()
    }

    override fun serialize(encoder: Encoder, value: MutableMeta) {
        encoder.encodeSerializableValue(MetaSerializer, value)
    }
}