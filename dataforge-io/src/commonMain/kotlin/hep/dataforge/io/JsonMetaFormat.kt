package hep.dataforge.io

import hep.dataforge.descriptors.ItemDescriptor
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.descriptors.ValueDescriptor
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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


object JsonMetaFormat : MetaFormat {

    override val name: String = "json"
    override val key: Short = 0x4a53//"JS"

    override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) {
        val str = meta.toJson().toString()
        writeText(str)
    }

    override fun Input.readMeta(descriptor: NodeDescriptor?): Meta {
        val str = readText()
        val json = Json.plain.parseJson(str)

        if (json is JsonObject) {
            return json.toMeta()
        } else {
            TODO("Non-object root not supported")
        }
    }
}

fun Value.toJson(descriptor: ValueDescriptor? = null): JsonElement {
    return if (isList()) {
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

//Use theese methods to customize JSON key mapping
private fun NameToken.toJsonKey(descriptor: ItemDescriptor?) = toString()
private fun NodeDescriptor?.getDescriptor(key: String) = this?.items?.get(key)

fun Meta.toJson(descriptor: NodeDescriptor? = null): JsonObject {

    //TODO search for same name siblings and arrange them into arrays
    val map = this.items.entries.associate {(name,item)->
        val itemDescriptor = descriptor?.items?.get(name.body)
        val key = name.toJsonKey(itemDescriptor)
        val value =          when (item) {
            is MetaItem.ValueItem -> {
                item.value.toJson(itemDescriptor as? ValueDescriptor)
            }
            is MetaItem.NodeItem -> {
                item.node.toJson(itemDescriptor as? NodeDescriptor)
            }
        }
        key to value
    }
    return JsonObject(map)
}


fun JsonObject.toMeta(descriptor: NodeDescriptor? = null) = JsonMeta(this, descriptor)

class JsonMeta(val json: JsonObject, val descriptor: NodeDescriptor? = null) : Meta {

    private fun JsonPrimitive.toValue(descriptor: ValueDescriptor?): Value {
        return when (this) {
            JsonNull -> Null
            else -> this.content.parseValue() // Optimize number and boolean parsing
        }
    }

    @Suppress("UNCHECKED_CAST")
    private operator fun MutableMap<String, MetaItem<JsonMeta>>.set(key: String, value: JsonElement): Unit {
        val itemDescriptor = descriptor.getDescriptor(key)
        //use name from descriptor in case descriptor name differs from json key
        val name = itemDescriptor?.name ?: key
        return when (value) {
            is JsonPrimitive -> {
                this[name] = MetaItem.ValueItem(value.toValue(itemDescriptor as? ValueDescriptor)) as MetaItem<JsonMeta>
            }
            is JsonObject -> {
                this[name] = MetaItem.NodeItem(value.toMeta(itemDescriptor as? NodeDescriptor))
            }
            is JsonArray -> {
                when {
                    value.all { it is JsonPrimitive } -> {
                        val listValue = ListValue(
                            value.map {
                                //We already checked that all values are primitives
                                (it as JsonPrimitive).toValue(itemDescriptor as? ValueDescriptor)
                            }
                        )
                        this[name] = MetaItem.ValueItem(listValue) as MetaItem<JsonMeta>
                    }
                    else -> value.forEachIndexed { index, jsonElement ->
                        when (jsonElement) {
                            is JsonObject -> {
                                this["$name[$index]"] =
                                    MetaItem.NodeItem(jsonElement.toMeta(itemDescriptor as? NodeDescriptor))
                            }
                            is JsonPrimitive -> {
                                this["$name[$index]"] =
                                    MetaItem.ValueItem(jsonElement.toValue(itemDescriptor as? ValueDescriptor))
                            }
                            is JsonArray -> TODO("Nested arrays not supported")
                        }
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