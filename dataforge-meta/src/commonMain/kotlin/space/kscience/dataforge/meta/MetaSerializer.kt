package space.kscience.dataforge.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.NameTokenSerializer

/**
 * Serialized for meta
 */
public object MetaSerializer : KSerializer<Meta> {

    private val itemsSerializer: KSerializer<Map<NameToken, Meta>> = MapSerializer(
        NameTokenSerializer,
        MetaSerializer
    )

    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Meta = JsonElement.serializer().deserialize(decoder).toMeta()

    override fun serialize(encoder: Encoder, value: Meta) {
        JsonElement.serializer().serialize(encoder, value.toJson())
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