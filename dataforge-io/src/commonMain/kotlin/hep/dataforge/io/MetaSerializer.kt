package hep.dataforge.io

import hep.dataforge.meta.Config
import hep.dataforge.meta.Meta
import hep.dataforge.meta.toConfig
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JsonObjectSerializer

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
object NameTokenSerializer: KSerializer<NameToken>

/**
 * Serialized for meta
 */
@Serializer(Meta::class)
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

@Serializer(Config::class)
object ConfigSerializer : KSerializer<Config> {
    override val descriptor: SerialDescriptor = JsonObjectSerializer.descriptor

    override fun deserialize(decoder: Decoder): Config {
        return JsonObjectSerializer.deserialize(decoder).toMeta().toConfig()
    }

    override fun serialize(encoder: Encoder, obj: Config) {
        JsonObjectSerializer.serialize(encoder, obj.toJson())
    }
}