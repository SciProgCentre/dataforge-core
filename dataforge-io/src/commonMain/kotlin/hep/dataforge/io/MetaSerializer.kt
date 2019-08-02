package hep.dataforge.io

import hep.dataforge.meta.Config
import hep.dataforge.meta.Meta
import hep.dataforge.meta.toConfig
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.json.JsonObjectSerializer

/**
 * Serialized for meta
 */
object MetaSerializer : KSerializer<Meta> {
    override val descriptor: SerialDescriptor = JsonObjectSerializer.descriptor

    override fun deserialize(decoder: Decoder): Meta {
        //currently just delegates serialization to json serializer
        return JsonObjectSerializer.deserialize(decoder).toMeta()
    }

    override fun serialize(encoder: Encoder, obj: Meta) {
        JsonObjectSerializer.serialize(encoder, obj.toJson())
    }
}

object ConfigSerializer: KSerializer<Config>{
    override val descriptor: SerialDescriptor = JsonObjectSerializer.descriptor

    override fun deserialize(decoder: Decoder): Config {
        return JsonObjectSerializer.deserialize(decoder).toMeta().toConfig()
    }

    override fun serialize(encoder: Encoder, obj: Config) {
        JsonObjectSerializer.serialize(encoder, obj.toJson())
    }
}