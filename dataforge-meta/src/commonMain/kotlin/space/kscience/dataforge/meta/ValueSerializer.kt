package space.kscience.dataforge.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object ValueSerializer : KSerializer<Value> {
    private val listSerializer by lazy { ListSerializer(ValueSerializer) }

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("space.kscience.dataforge.meta.Value") {
            element<Boolean>("isList")
            element<ValueType>("valueType")
            element<String>("value")
        }

    private fun Decoder.decodeValue(): Value {
        return when (decodeSerializableValue(ValueType.serializer())) {
            ValueType.NULL -> Null
            ValueType.NUMBER -> decodeDouble().asValue() //TODO differentiate?
            ValueType.BOOLEAN -> decodeBoolean().asValue()
            ValueType.STRING -> decodeString().asValue()
            ValueType.LIST -> decodeSerializableValue(ListSerializer(ValueSerializer)).asValue()
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
        encodeSerializableValue(ValueType.serializer(), value.type)
        when (value.type) {
            ValueType.NULL -> {
                // do nothing
            }
            ValueType.NUMBER -> encodeDouble(value.double)
            ValueType.BOOLEAN -> encodeBoolean(value.boolean)
            ValueType.STRING -> encodeString(value.string)
            ValueType.LIST -> encodeSerializableValue(ListSerializer(ValueSerializer),value.list)
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