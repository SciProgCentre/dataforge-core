package hep.dataforge.meta

import hep.dataforge.names.NameToken
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.json.JsonOutput


private class DeserializedMeta(override val items: Map<NameToken, MetaItem<Meta>>) : MetaBase()

/**
 * Serialized for meta
 */
@Serializer(Meta::class)
object MetaSerializer : KSerializer<Meta> {
    private val mapSerializer = MapSerializer(
        NameToken.serializer(),
        MetaItem.serializer(MetaSerializer)
    )

    override val descriptor: SerialDescriptor get() = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): Meta {
        return if (decoder is JsonInput) {
            JsonObjectSerializer.deserialize(decoder).toMeta()
        } else {
            DeserializedMeta(mapSerializer.deserialize(decoder))
        }
    }

    override fun serialize(encoder: Encoder, value: Meta) {
        if (encoder is JsonOutput) {
            JsonObjectSerializer.serialize(encoder, value.toJson())
        } else {
            mapSerializer.serialize(encoder, value.items)
        }
    }
}

@Serializer(Config::class)
object ConfigSerializer : KSerializer<Config> {
    override val descriptor: SerialDescriptor get() = MetaSerializer.descriptor

    override fun deserialize(decoder: Decoder): Config {
        return MetaSerializer.deserialize(decoder).asConfig()
    }

    override fun serialize(encoder: Encoder, value: Config) {
        MetaSerializer.serialize(encoder, value)
    }
}