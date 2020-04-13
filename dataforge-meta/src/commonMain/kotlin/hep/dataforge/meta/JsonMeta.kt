@file:Suppress("UNUSED_PARAMETER")

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
private fun String.toJsonKey(descriptor: ItemDescriptor?) = descriptor?.attributes["jsonName"].string ?: toString()

//private fun NodeDescriptor?.getDescriptor(key: String) = this?.items?.get(key)

/**
 * Convert given [Meta] to [JsonObject]. Primitives and nodes are copied as is, same name siblings are treated as json arrays
 */
fun Meta.toJson(descriptor: NodeDescriptor? = null, index: String? = null): JsonObject {

    val elementMap = HashMap<String, JsonElement>()

    fun MetaItem<*>.toJsonElement(itemDescriptor: ItemDescriptor?, index: String? = null): JsonElement = when (this) {
        is MetaItem.ValueItem -> {
            value.toJson(itemDescriptor as? ValueDescriptor)
        }
        is MetaItem.NodeItem -> {
            node.toJson(itemDescriptor as? NodeDescriptor, index)
        }
    }

    fun addElement(key: String) {
        val itemDescriptor = descriptor?.items?.get(key)
        val jsonKey = key.toJsonKey(itemDescriptor)
        val items = getIndexed(key)
        when (items.size) {
            0 -> {
                //do nothing
            }
            1 -> {
                elementMap[jsonKey] = items.values.first().toJsonElement(itemDescriptor)
            }
            else -> {
                val array = jsonArray {
                    items.forEach { (index, item) ->
                        +item.toJsonElement(itemDescriptor, index)
                    }
                }
                elementMap[jsonKey] = array
            }
        }
    }

    ((descriptor?.items?.keys ?: emptySet()) + items.keys.map { it.body }).forEach(::addElement)


    if (index != null) {
        elementMap["@index"] = JsonPrimitive(index)
    }

    return JsonObject(elementMap)

//    // use descriptor keys in the order they are declared
//    val keys = (descriptor?.items?.keys ?: emptySet()) + this.items.keys.map { it.body }
//
//    //TODO search for same name siblings and arrange them into arrays
//    val map = this.items.entries.associate { (name, item) ->
//        val itemDescriptor = descriptor?.items?.get(name.body)
//        val key = name.toJsonKey(itemDescriptor)
//        val value = when (item) {
//            is MetaItem.ValueItem -> {
//                item.value.toJson(itemDescriptor as? ValueDescriptor)
//            }
//            is MetaItem.NodeItem -> {
//                item.node.toJson(itemDescriptor as? NodeDescriptor)
//            }
//        }
//        key to value
//    }
//    return JsonObject(map)
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

/**
 * A meta wrapping json object
 */
class JsonMeta(val json: JsonObject, val descriptor: NodeDescriptor? = null) : MetaBase() {

    @Suppress("UNCHECKED_CAST")
    private operator fun MutableMap<String, MetaItem<JsonMeta>>.set(key: String, value: JsonElement): Unit {
        val itemDescriptor = descriptor?.items?.get(key)
        when (value) {
            is JsonPrimitive -> {
                this[key] =
                    MetaItem.ValueItem(value.toValue(itemDescriptor as? ValueDescriptor)) as MetaItem<JsonMeta>
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
        val map = LinkedHashMap<String, MetaItem<JsonMeta>>()
        json.forEach { (key, value) -> map[key] = value }
        map.mapKeys { it.key.toName().first()!! }
    }
}