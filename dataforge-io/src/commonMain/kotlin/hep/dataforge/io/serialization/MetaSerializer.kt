package hep.dataforge.io

import hep.dataforge.io.serialization.descriptor
import hep.dataforge.meta.*
import hep.dataforge.names.NameToken
import hep.dataforge.values.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.json.JsonOutput


@Serializer(Value::class)
object ValueSerializer : KSerializer<Value> {
    private val valueTypeSerializer = EnumSerializer(ValueType::class)
    private val listSerializer by lazy { ArrayListSerializer(ValueSerializer) }

    override val descriptor: SerialDescriptor = descriptor("Value") {
        boolean("isList")
        enum<ValueType>("valueType")
        element("value", PolymorphicClassDescriptor)
    }

    private fun Decoder.decodeValue(): Value {
        return when (decode(valueTypeSerializer)) {
            ValueType.NULL -> Null
            ValueType.NUMBER -> decodeDouble().asValue() //TODO differentiate?
            ValueType.BOOLEAN -> decodeBoolean().asValue()
            ValueType.STRING -> decodeString().asValue()
            else -> decodeString().parseValue()
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
        encode(valueTypeSerializer, value.type)
        when (value.type) {
            ValueType.NULL -> {
                // do nothing
            }
            ValueType.NUMBER -> encodeDouble(value.double)
            ValueType.BOOLEAN -> encodeBoolean(value.boolean)
            ValueType.STRING -> encodeString(value.string)
            else -> encodeString(value.string)
        }
    }

    override fun serialize(encoder: Encoder, obj: Value) {
        encoder.encodeBoolean(obj.isList())
        if (obj.isList()) {
            listSerializer.serialize(encoder, obj.list)
        } else {
            encoder.encodeValue(obj)
        }
    }
}

@Serializer(MetaItem::class)
object MetaItemSerializer : KSerializer<MetaItem<*>> {
    override val descriptor: SerialDescriptor = descriptor("MetaItem") {
        boolean("isNode")
        element("value", PolymorphicClassDescriptor)
    }


    override fun deserialize(decoder: Decoder): MetaItem<*> {
        val isNode = decoder.decodeBoolean()
        return if (isNode) {
            MetaItem.NodeItem(decoder.decode(MetaSerializer))
        } else {
            MetaItem.ValueItem(decoder.decode(ValueSerializer))
        }
    }

    override fun serialize(encoder: Encoder, obj: MetaItem<*>) {
        encoder.encodeBoolean(obj is MetaItem.NodeItem)
        when (obj) {
            is MetaItem.NodeItem -> MetaSerializer.serialize(encoder, obj.node)
            is MetaItem.ValueItem -> ValueSerializer.serialize(encoder, obj.value)
        }
    }
}

private class DeserializedMeta(override val items: Map<NameToken, MetaItem<*>>) : MetaBase()


/**
 * Serialized for meta
 */
@Serializer(Meta::class)
object MetaSerializer : KSerializer<Meta> {
    private val mapSerializer =
        HashMapSerializer(StringSerializer, MetaItemSerializer)

    override val descriptor: SerialDescriptor =
        NamedMapClassDescriptor("Meta", StringSerializer.descriptor, MetaItemSerializer.descriptor)

    override fun deserialize(decoder: Decoder): Meta {
        return if (decoder is JsonInput) {
            JsonObjectSerializer.deserialize(decoder).toMeta()
        } else {
            DeserializedMeta(mapSerializer.deserialize(decoder).mapKeys { NameToken(it.key) })
        }
    }

    override fun serialize(encoder: Encoder, obj: Meta) {
        if (encoder is JsonOutput) {
            JsonObjectSerializer.serialize(encoder, obj.toJson())
        } else {
            mapSerializer.serialize(encoder, obj.items.mapKeys { it.key.toString() })
        }
    }
}

@Serializer(Config::class)
object ConfigSerializer : KSerializer<Config> {
    override val descriptor: SerialDescriptor = MetaSerializer.descriptor

    override fun deserialize(decoder: Decoder): Config {
        return MetaSerializer.deserialize(decoder).toConfig()
    }

    override fun serialize(encoder: Encoder, obj: Config) {
        MetaSerializer.serialize(encoder, obj)
    }
}