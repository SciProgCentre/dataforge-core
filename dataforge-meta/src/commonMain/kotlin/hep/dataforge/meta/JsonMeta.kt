package hep.dataforge.meta

import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.descriptors.ValueDescriptor
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName
import hep.dataforge.values.*
import kotlinx.serialization.json.*


/**
 * @param descriptor reserved for custom serialization in future
 */
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

//Use these methods to customize JSON key mapping
private fun NameToken.toJsonKey(descriptor: ItemDescriptor?) = toString()

//private fun NodeDescriptor?.getDescriptor(key: String) = this?.items?.get(key)

fun Meta.toJson(descriptor: NodeDescriptor? = null): JsonObject {

    //TODO search for same name siblings and arrange them into arrays
    val map = this.items.entries.associate { (name, item) ->
        val itemDescriptor = descriptor?.items?.get(name.body)
        val key = name.toJsonKey(itemDescriptor)
        val value = when (item) {
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

fun JsonObject.toMeta(descriptor: NodeDescriptor? = null): Meta = JsonMeta(this, descriptor)

fun JsonPrimitive.toValue(descriptor: ValueDescriptor?): Value {
    return when (this) {
        JsonNull -> Null
        else -> this.content.parseValue() // Optimize number and boolean parsing
    }
}

fun JsonElement.toMetaItem(descriptor: ItemDescriptor? = null): MetaItem<JsonMeta> = when (this) {
    is JsonPrimitive -> {
        val value = this.toValue(descriptor as? ValueDescriptor)
        MetaItem.ValueItem(value)
    }
    is JsonObject -> {
        val meta = JsonMeta(this, descriptor as? NodeDescriptor)
        MetaItem.NodeItem(meta)
    }
    is JsonArray -> {
        if (this.all { it is JsonPrimitive }) {
            val value = if (isEmpty()) {
                Null
            } else {
                ListValue(
                    map<JsonElement, Value> {
                        //We already checked that all values are primitives
                        (it as JsonPrimitive).toValue(descriptor as? ValueDescriptor)
                    }
                )
            }
            MetaItem.ValueItem(value)
        } else {
            json {
                "@value" to this@toMetaItem
            }.toMetaItem(descriptor)
        }
    }
}

class JsonMeta(val json: JsonObject, val descriptor: NodeDescriptor? = null) : MetaBase() {

    @Suppress("UNCHECKED_CAST")
    private operator fun MutableMap<String, MetaItem<JsonMeta>>.set(key: String, value: JsonElement): Unit {
        val itemDescriptor = descriptor?.items?.get(key)
        return when (value) {
            is JsonPrimitive -> {
                this[key] = MetaItem.ValueItem(value.toValue(itemDescriptor as? ValueDescriptor)) as MetaItem<JsonMeta>
            }
            is JsonObject -> {
                this[key] = MetaItem.NodeItem(
                    JsonMeta(
                        value,
                        itemDescriptor as? NodeDescriptor
                    )
                )
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
                        this[key] = MetaItem.ValueItem(listValue) as MetaItem<JsonMeta>
                    }
                    else -> value.forEachIndexed { index, jsonElement ->
                        this["$key[$index]"] = jsonElement.toMetaItem(itemDescriptor)
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