package hep.dataforge.values

import hep.dataforge.meta.boolean
import hep.dataforge.meta.enum
import hep.dataforge.meta.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(Value::class)
object ValueSerializer : KSerializer<Value> {
    private val listSerializer by lazy { ListSerializer(ValueSerializer) }

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("hep.dataforge.values.Value") {
            boolean("isList")
            enum<ValueType>("valueType")
            string("value")
        }

    private fun Decoder.decodeValue(): Value {
        return when (decodeSerializableValue(ValueType.serializer())) {
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
        encodeSerializableValue(ValueType.serializer(), value.type)
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