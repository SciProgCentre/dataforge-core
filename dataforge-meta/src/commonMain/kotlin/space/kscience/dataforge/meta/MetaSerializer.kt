package space.kscience.dataforge.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

/**
 * Serialized for [Meta]
 */
public object MetaSerializer : KSerializer<Meta> {
    private val genericMetaSerializer = SealedMeta.serializer()

    override val descriptor: SerialDescriptor = genericMetaSerializer.descriptor

    override fun deserialize(decoder: Decoder): Meta = if (decoder is JsonDecoder) {
        decoder.decodeJsonElement().toMeta()
    } else {
        genericMetaSerializer.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: Meta) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(value.toJson())
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