package hep.dataforge.meta

import kotlinx.serialization.json.*


/*Universal serialization*/

//sealed class MetaItemProxy {
//
//    @Serializable
//    class NumberValueProxy(val number: Number) : MetaItemProxy()
//
//    @Serializable
//    class StringValueProxy(val string: String) : MetaItemProxy()
//
//    @Serializable
//    class BooleanValueProxy(val boolean: Boolean) : MetaItemProxy()
//
//    @Serializable
//    object NullValueProxy : MetaItemProxy()
//
//    @Serializable
//    class MetaProxy(@Serializable val map: Map<String, MetaItemProxy>) : MetaItemProxy()
//
//    @Serializable
//    class MetaListProxy(@Serializable val nodes: List<MetaProxy>) : MetaItemProxy()
//}
//
//
//fun Meta.toMap(): Map<String, MetaItemProxy> {
//   return this.items.mapValues { (_, value) ->
//        when (value) {
//            is MetaItem.ValueItem -> when (value.value.type) {
//                ValueType.NUMBER -> MetaItemProxy.NumberValueProxy(value.value.number)
//                ValueType.STRING -> MetaItemProxy.StringValueProxy(value.value.string)
//                ValueType.BOOLEAN -> MetaItemProxy.BooleanValueProxy(value.value.boolean)
//                ValueType.NULL -> MetaItemProxy.NullValueProxy
//            }
//            is MetaItem.SingleNodeItem -> MetaItemProxy.MetaProxy(value.node.toMap())
//            is MetaItem.MultiNodeItem -> MetaItemProxy.MetaListProxy(value.nodes.map { MetaItemProxy.MetaProxy(it.toMap()) })
//        }
//    }
//}


/*Direct JSON serialization*/

fun Value.toJson(): JsonElement = if (isList()) {
    JsonArray(list.map { it.toJson() })
} else {
    when (type) {
        ValueType.NUMBER -> JsonPrimitive(number)
        ValueType.STRING -> JsonPrimitive(string)
        ValueType.BOOLEAN -> JsonPrimitive(boolean)
        ValueType.NULL -> JsonNull
    }
}

fun Meta.toJSON(): JsonObject {
    val map = this.items.mapValues { (_, value) ->
        when (value) {
            is MetaItem.ValueItem -> value.value.toJson()
            is MetaItem.SingleNodeItem -> value.node.toJSON()
            is MetaItem.MultiNodeItem -> JsonArray(value.nodes.map { it.toJSON() })
        }
    }
    return JsonObject(map)
}

fun JsonPrimitive.toValue(): Value {
    return when (this) {
        is JsonLiteral -> LazyParsedValue(content)
        is JsonNull -> Null
    }
}

fun JsonObject.toMeta(): Meta {
    return buildMeta {
        this@toMeta.forEach { (key, value) ->
            when (value) {
                is JsonPrimitive -> set(key, value.toValue())
                is JsonObject -> set(key, value.toMeta())
                is JsonArray -> if (value.all { it is JsonPrimitive }) {
                    set(key, ListValue(value.map { (it as JsonPrimitive).toValue() }))
                } else {
                    set(
                            key,
                            value.map {
                                if (it is JsonObject) {
                                    it.toMeta()
                                } else {
                                    buildMeta { "@value" to it.primitive.toValue() }
                                }
                            }
                    )
                }
            }
        }
    }
}

/*Direct CBOR serialization*/

//fun Meta.toBinary(out: OutputStream) {
//    fun CBOR.CBOREncoder.encodeChar(char: Char) {
//        encodeNumber(char.toByte().toLong())
//    }
//
//    fun CBOR.CBOREncoder.encodeValue(value: Value) {
//        if (value.isList()) {
//            encodeChar('L')
//            startArray()
//            value.list.forEach {
//                encodeValue(it)
//            }
//            end()
//        } else when (value.type) {
//            ValueType.NUMBER -> when (value.value) {
//                is Int, is Short, is Long -> {
//                    encodeChar('i')
//                    encodeNumber(value.number.toLong())
//                }
//                is Float -> {
//                    encodeChar('f')
//                    encodeFloat(value.number.toFloat())
//                }
//                else -> {
//                    encodeChar('d')
//                    encodeDouble(value.number.toDouble())
//                }
//            }
//            ValueType.STRING -> {
//                encodeChar('S')
//                encodeString(value.string)
//            }
//            ValueType.BOOLEAN -> {
//                if (value.boolean) {
//                    encodeChar('+')
//                } else {
//                    encodeChar('-')
//                }
//            }
//            ValueType.NULL -> {
//                encodeChar('N')
//            }
//        }
//    }
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