package hep.dataforge.meta

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl

@Serializer(forClass = Value::class)
object ValueSerializer : KSerializer<Value> {
    override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("Value")

    override fun load(input: KInput): Value {
        val key = input.readByteValue()
        return when (key.toChar()) {
            'S' -> StringValue(input.readStringValue())
            'd' -> NumberValue(input.readDoubleValue())
            'f' -> NumberValue(input.readFloatValue())
            'i' -> NumberValue(input.readIntValue())
            's' -> NumberValue(input.readShortValue())
            'l' -> NumberValue(input.readLongValue())
            'b' -> NumberValue(input.readByteValue())
            '+' -> True
            '-' -> False
            'N' -> Null
            'L' -> {
                val size = input.readIntValue()
                val list = (0 until size).map { load(input) }
                ListValue(list)
            }
            else -> error("Unknown value deserialization ket '$key'")
        }
    }

    override fun save(output: KOutput, obj: Value) {
        when (obj.type) {
            ValueType.NUMBER -> {
                val number = obj.number
                when (number) {
                    is Float -> {
                        output.writeByteValue('f'.toByte())
                        output.writeFloatValue(number)
                    }
                    is Short -> {
                        output.writeByteValue('s'.toByte())
                        output.writeShortValue(number)
                    }
                    is Int -> {
                        output.writeByteValue('i'.toByte())
                        output.writeIntValue(number)
                    }
                    is Long -> {
                        output.writeByteValue('l'.toByte())
                        output.writeLongValue(number)
                    }
                    is Byte -> {
                        output.writeByteValue('b'.toByte())
                        output.writeByteValue(number)
                    }
                    is Double -> {
                        output.writeByteValue('d'.toByte())
                        output.writeDoubleValue(number)
                    }
                    else -> {
                        //TODO add warning
                        output.writeByteValue('d'.toByte())
                        output.writeDoubleValue(number.toDouble())
                    }
                }
            }
            ValueType.STRING -> {
                output.writeByteValue('S'.toByte())
                output.writeStringValue(obj.string)
            }
            ValueType.BOOLEAN -> if (obj.boolean) {
                output.writeByteValue('+'.toByte())
            } else {
                output.writeByteValue('-'.toByte())
            }
            ValueType.NULL -> output.writeByteValue('N'.toByte())
        }
    }
}


//@Serializer(forClass = Meta::class)
//object MetaSerializer: KSerializer<Meta>{
//    override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("Meta")
//
//    override fun load(input: KInput): Meta {
//
//    }
//
//    override fun save(output: KOutput, obj: Meta) {
//        NamedValueOutput()
//    }
//
//}