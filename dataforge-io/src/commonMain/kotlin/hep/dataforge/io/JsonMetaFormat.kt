package hep.dataforge.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName
import hep.dataforge.values.*
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.core.readText
import kotlinx.io.core.writeText
import kotlinx.serialization.json.*


object JsonMetaFormat : MetaFormat {

    override fun write(obj: Meta, out: Output) {
        val str = obj.toJson().toString()
        out.writeText(str)
    }

    override fun read(input: Input): Meta {
        val str = input.readText()
        val json = Json.plain.parseJson(str)

        if (json is JsonObject) {
            return json.toMeta()
        } else {
            TODO("Non-object root not supported")
        }
    }
}

fun Value.toJson(): JsonElement {
    return if(isList()){
        JsonArray(list.map { it.toJson() })
    } else {
        when (type) {
            ValueType.NUMBER -> JsonPrimitive(number)
            ValueType.STRING -> JsonPrimitive(string)
            ValueType.BOOLEAN -> JsonPrimitive(boolean)
            ValueType.NULL -> JsonNull
        }
    }
}

fun Meta.toJson(): JsonObject {
    val map = this.items.mapValues { entry ->
        val value = entry.value
        when (value) {
            is MetaItem.ValueItem -> value.value.toJson()
            is MetaItem.NodeItem -> value.node.toJson()
        }
    }.mapKeys { it.key.toString() }
    return JsonObject(map)
}


fun JsonObject.toMeta() = JsonMeta(this)

class JsonMeta(val json: JsonObject) : Meta {

    private fun JsonPrimitive.toValue(): Value {
        return when (this) {
            JsonNull -> Null
            else -> this.content.parseValue() // Optimize number and boolean parsing
        }
    }

    private operator fun MutableMap<String, MetaItem<JsonMeta>>.set(key: String, value: JsonElement) = when (value) {
        is JsonPrimitive -> this[key] = MetaItem.ValueItem(value.toValue())
        is JsonObject -> this[key] = MetaItem.NodeItem(value.toMeta())
        is JsonArray -> {
            when {
                value.all { it is JsonPrimitive } -> {
                    val listValue = ListValue(
                        value.map {
                            //We already checked that all values are primitives
                            (it as JsonPrimitive).toValue()
                        }
                    )
                    this[key] = MetaItem.ValueItem(listValue)
                }
                else -> value.forEachIndexed { index, jsonElement ->
                    when (jsonElement) {
                        is JsonObject -> this["$key[$index]"] = MetaItem.NodeItem(JsonMeta(jsonElement))
                        is JsonPrimitive -> this["$key[$index]"] = MetaItem.ValueItem(jsonElement.toValue())
                        is JsonArray -> TODO("Nested arrays not supported")
                    }
                }
            }
        }
    }

    override val items: Map<NameToken, MetaItem<JsonMeta>> by lazy {
        val map = HashMap<String, MetaItem<JsonMeta>>()
        json.forEach { (key, value) -> map[key] = value }
        map.mapKeys { it.key.toName().first()!! }
    }
}

class JsonMetaFormatFactory : MetaFormatFactory {
    override val name: String = "json"
    override val key: Short = 0x4a53//"JS"

    override fun build() = JsonMetaFormat
}