package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.names.NameToken
import hep.dataforge.values.*
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.core.readText
import kotlinx.io.core.writeText
import kotlinx.serialization.json.*


object JsonMetaFormat : MetaFormat {

    override fun write(meta: Meta, out: Output) {
        val str = meta.toJson().toString()
        out.writeText(str)
    }

    override fun read(input: Input): Meta {
        val str = input.readText()
        val json = Json.plain.parseJson(str)

        if(json is JsonObject) {
            return json.toMeta()
        } else {
            TODO("non-object root")
        }
    }
}

fun Value.toJson(): JsonElement {
    return when (type) {
        ValueType.NUMBER -> JsonPrimitive(number)
        ValueType.STRING -> JsonPrimitive(string)
        ValueType.BOOLEAN -> JsonPrimitive(boolean)
        ValueType.NULL -> JsonNull
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


fun JsonElement.toMetaItem() = when (this) {
    is JsonPrimitive -> MetaItem.ValueItem<JsonMeta>(this.toValue())
    is JsonObject -> MetaItem.NodeItem(this.toMeta())
    is JsonArray -> {
        if (this.all { it is JsonPrimitive }) {
            val value = ListValue(this.map { (it as JsonPrimitive).toValue() })
            MetaItem.ValueItem<JsonMeta>(value)
        } else {
            TODO("mixed nodes json")
        }
    }
}

fun JsonObject.toMeta() = JsonMeta(this)

private fun JsonPrimitive.toValue(): Value {
    return when (this) {
        JsonNull -> Null
        else -> this.content.parseValue() // Optimize number and boolean parsing
    }
}

class JsonMeta(val json: JsonObject) : Meta {
    override val items: Map<NameToken, MetaItem<out Meta>> by lazy {
        json.mapKeys { NameToken(it.key) }.mapValues { entry ->
            entry.value.toMetaItem()
        }
    }
}

class JsonMetaFormatFactory : MetaFormatFactory {
    override val name: String = "json"
    override val key: Short = 0x4a53//"JS"

    override fun build() = JsonMetaFormat
}