package hep.dataforge.meta.io

import hep.dataforge.meta.*
import kotlinx.serialization.json.*


/*Direct JSON serialization*/

//fun Value.toJson(): JsonElement = if (isList()) {
//    JsonArray(list.map { it.toJson() })
//} else {
//    when (type) {
//        ValueType.NUMBER -> JsonPrimitive(number)
//        ValueType.STRING -> JsonPrimitive(string)
//        ValueType.BOOLEAN -> JsonPrimitive(boolean)
//        ValueType.NULL -> JsonNull
//    }
//}
//
//fun Meta.toJSON(): JsonObject {
//    val map = this.items.mapValues { (_, value) ->
//        when (value) {
//            is MetaItem.ValueItem -> value.value.toJson()
//            is MetaItem.SingleNodeItem -> value.node.toJSON()
//            is MetaItem.MultiNodeItem -> JsonArray(value.nodes.map { it.toJSON() })
//        }
//    }
//    return JsonObject(map)
//}
//
//fun JsonPrimitive.toValue(): Value {
//    return when (this) {
//        is JsonLiteral -> LazyParsedValue(content)
//        is JsonNull -> Null
//    }
//}
//
//fun JsonObject.toMeta(): Meta {
//    return buildMeta {
//        this@toMeta.forEach { (key, value) ->
//            when (value) {
//                is JsonPrimitive -> set(key, value.toValue())
//                is JsonObject -> set(key, value.toMeta())
//                is JsonArray -> if (value.all { it is JsonPrimitive }) {
//                    set(key, ListValue(value.map { (it as JsonPrimitive).toValue() }))
//                } else {
//                    set(
//                            key,
//                            value.map {
//                                if (it is JsonObject) {
//                                    it.toMeta()
//                                } else {
//                                    buildMeta { "@value" to it.primitive.toValue() }
//                                }
//                            }
//                    )
//                }
//            }
//        }
//    }
//}

/*Direct CBOR serialization*/

//fun Meta.toBinary(out: OutputStream) {
//    fun CBOR.CBOREncoder.encodeChar(char: Char) {
//        encodeNumber(char.toByte().toLong())
//    }
//
//
//    fun CBOR.CBOREncoder.encodeMeta(meta: Meta) {
//        meta.items.forEach { (key, item) ->
//            this.startMap()
//            encodeString(key)
//            when (item) {
//                is MetaItem.ValueItem -> {
//                    encodeChar('V')
//                    encodeValue(item.value)
//                }
//                is MetaItem.SingleNodeItem -> {
//                    startArray()
//                    encodeMeta(item.node)
//                }
//                is MetaItem.MultiNodeItem -> {
//                    startArray()
//                    item.nodes.forEach {
//                        encodeMeta(it)
//                    }
//                    end()
//                }
//            }
//        }
//    }
//
//
//    CBOR.CBOREncoder(out).apply {
//        encodeMeta(this@toBinary)
//    }
//}
//
//fun InputStream.readBinaryMeta(): Meta {
//    fun CBOR.CBORDecoder.nextChar(): Char = nextNumber().toByte().toChar()
//
//    fun CBOR.CBORDecoder.nextValue(): Value {
//        val key = nextChar()
//        return when(key){
//            'L' -> {
//                val size = startArray()
//                val res = (0 until size).map { nextValue() }
//                end()
//                ListValue(res)
//            }
//            'S' -> StringValue(nextString())
//            'N' -> Null
//            '+' -> True
//            '-' -> False
//            'i' -> NumberValue(nextNumber())
//            'f' -> NumberValue(nextFloat())
//            'd' -> NumberValue(nextDouble())
//            else -> error("Unknown binary key: $key")
//        }
//    }
//
//    fun CBOR.CBORDecoder.nextMeta(): Meta{
//
//    }
//
//}