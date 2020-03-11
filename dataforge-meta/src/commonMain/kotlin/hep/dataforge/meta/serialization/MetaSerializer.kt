package hep.dataforge.meta.serialization

import hep.dataforge.meta.*
import hep.dataforge.names.NameToken
import hep.dataforge.values.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.json.JsonOutput


@Serializer(Value::class)
@OptIn(InternalSerializationApi::class)
object ValueSerializer : KSerializer<Value> {
    //    private val valueTypeSerializer = EnumSerializer(ValueType::class)
    private val listSerializer by lazy { ValueSerializer.list }

    override val descriptor: SerialDescriptor = SerialDescriptor("Value") {
        boolean("isList")
        enum<ValueType>("valueType")
        string("value")
    }

    private fun Decoder.decodeValue(): Value {
        return when (decode(ValueType.serializer())) {
            ValueType.NULL -> Null
            ValueType.NUMBER -> decodeDouble().asValue() //TODO differentiate?
            ValueType.BOOLEAN -> decodeBoolean().asValue()
            ValueType.STRING -> decodeString().asValue()
        }
    }


    override fun deserialize(decoder: Decoder): Value {
        val isList = decoder.decodeBoolean()
        return if (isList) {
            listSerializer.deserialize(decoder).asValue()
        } else {
            decoder.decodeValue()
        }
    }

    private fun Encoder.encodeValue(value: Value) {
        encode(ValueType.serializer(), value.type)
        when (value.type) {
            ValueType.NULL -> {
                // do nothing
            }
            ValueType.NUMBER -> encodeDouble(value.double)
            ValueType.BOOLEAN -> encodeBoolean(value.boolean)
            ValueType.STRING -> encodeString(value.string)
        }
    }

    override fun serialize(encoder: Encoder, value: Value) {
        encoder.encodeBoolean(value.isList())
        if (value.isList()) {
            listSerializer.serialize(encoder, value.list)
        } else {
            encoder.encodeValue(value)
        }
    }
}

private class DeserializedMeta(override val items: Map<NameToken, MetaItem<*>>) : MetaBase()

/**
 * Serialized for meta
 */
@Serializer(Meta::class)
object MetaSerializer : KSerializer<Meta> {
    private val mapSerializer = MapSerializer(
        String.serializer(),
        MetaItem.serializer(MetaSerializer)
    )

    override val descriptor: SerialDescriptor get() = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): Meta {
        return if (decoder is JsonInput) {
            JsonObjectSerializer.deserialize(decoder).toMeta()
        } else {
            DeserializedMeta(mapSerializer.deserialize(decoder).mapKeys { NameToken(it.key) })
        }
    }

    override fun serialize(encoder: Encoder, value: Meta) {
        if (encoder is JsonOutput) {
            JsonObjectSerializer.serialize(encoder, value.toJson())
        } else {
            mapSerializer.serialize(encoder, value.items.mapKeys { it.key.toString() })
        }
    }
}

@Serializer(Config::class)
object ConfigSerializer : KSerializer<Config> {
    override val descriptor: SerialDescriptor = MetaSerializer.descriptor

    override fun deserialize(decoder: Decoder): Config {
        return MetaSerializer.deserialize(decoder).asConfig()
    }

    override fun serialize(encoder: Encoder, value: Config) {
        MetaSerializer.serialize(encoder, value)
    }
}