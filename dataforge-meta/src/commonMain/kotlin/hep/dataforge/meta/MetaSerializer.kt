package hep.dataforge.meta

import hep.dataforge.names.NameToken
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Serialized for meta
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializer(Meta::class)
public object MetaSerializer : KSerializer<Meta> {

    private val mapSerializer = MapSerializer(
        NameToken.serializer(),
        MetaItem.serializer(MetaSerializer)
    )

    override val descriptor: SerialDescriptor get() = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): Meta {
        return if (decoder is JsonDecoder) {
            JsonObject.serializer().deserialize(decoder).toMeta()
        } else {
            object : MetaBase() {
                override val items: Map<NameToken, MetaItem<*>> = mapSerializer.deserialize(decoder)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Meta) {
        if (encoder is JsonEncoder) {
            JsonObject.serializer().serialize(encoder, value.toJson())
        } else {
            mapSerializer.serialize(encoder, value.items)
        }
    }
}