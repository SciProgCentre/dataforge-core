@file:Suppress("UNUSED_PARAMETER")

package hep.dataforge.meta

import hep.dataforge.meta.JsonMeta.Companion.JSON_ARRAY_KEY
import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.descriptors.ValueDescriptor
import hep.dataforge.names.NameToken
import hep.dataforge.names.withIndex
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
private fun Meta.toJsonWithIndex(descriptor: NodeDescriptor?, indexValue: String?): JsonObject {

    val elementMap = HashMap<String, JsonElement>()

    fun MetaItem<*>.toJsonElement(itemDescriptor: ItemDescriptor?, index: String?): JsonElement = when (this) {
        is MetaItem.ValueItem -> {
            value.toJson(itemDescriptor as? ValueDescriptor)
        }
        is MetaItem.NodeItem -> {
            node.toJsonWithIndex(itemDescriptor as? NodeDescriptor, index)
        }
    }

    fun addElement(key: String) {
        val itemDescriptor = descriptor?.items?.get(key)
        val jsonKey = key.toJsonKey(itemDescriptor)
        val items: Map<String?, MetaItem<*>> = getIndexed(key)
        when (items.size) {
            0 -> {
                //do nothing
            }
            1 -> {
                elementMap[jsonKey] = items.values.first().toJsonElement(itemDescriptor, null)
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


    if (indexValue != null) {
        val indexKey = descriptor?.indexKey ?: NodeDescriptor.DEFAULT_INDEX_KEY
        elementMap[indexKey] = JsonPrimitive(indexValue)
    }

    return JsonObject(elementMap)
}

fun Meta.toJson(descriptor: NodeDescriptor? = null): JsonObject = toJsonWithIndex(descriptor, null)

fun JsonObject.toMeta(descriptor: NodeDescriptor? = null): JsonMeta = JsonMeta(this, descriptor)

fun JsonPrimitive.toValue(descriptor: ValueDescriptor?): Value {
    return when (this) {
        JsonNull -> Null
        is JsonLiteral -> {
            when (body) {
                true -> True
                false -> False
                is Number -> NumberValue(body as Number)
                else -> if (isString) {
                    StringValue(content)
                } else {
                    content.parseValue()
                }
            }
        }
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
                map<JsonElement, Value> {
                    //We already checked that all values are primitives
                    (it as JsonPrimitive).toValue(descriptor as? ValueDescriptor)
                }.asValue()
            }
            MetaItem.ValueItem(value)
        } else {
            //We can't return multiple items therefore we create top level node
            json { JSON_ARRAY_KEY to this@toMetaItem }.toMetaItem(descriptor)
        }
    }
}

/**
 * A meta wrapping json object
 */
class JsonMeta(val json: JsonObject, val descriptor: NodeDescriptor? = null) : MetaBase() {

    private fun buildItems(): Map<NameToken, MetaItem<JsonMeta>> {
        val map = LinkedHashMap<NameToken, MetaItem<JsonMeta>>()

        json.forEach { (jsonKey, value) ->
            val key = NameToken(jsonKey)
            val itemDescriptor = descriptor?.items?.get(jsonKey)
            when (value) {
                is JsonPrimitive -> {
                    map[key] = MetaItem.ValueItem(value.toValue(itemDescriptor as? ValueDescriptor))
                }
                is JsonObject -> {
                    map[key] = MetaItem.NodeItem(
                        JsonMeta(
                            value,
                            itemDescriptor as? NodeDescriptor
                        )
                    )
                }
                is JsonArray -> if (value.all { it is JsonPrimitive }) {
                    val listValue = ListValue(
                        value.map {
                            //We already checked that all values are primitives
                            (it as JsonPrimitive).toValue(itemDescriptor as? ValueDescriptor)
                        }
                    )
                    map[key] = MetaItem.ValueItem(listValue)
                } else value.forEachIndexed { index, jsonElement ->
                    val indexKey = (itemDescriptor as? NodeDescriptor)?.indexKey ?: NodeDescriptor.DEFAULT_INDEX_KEY
                    val indexValue: String = (jsonElement as? JsonObject)
                        ?.get(indexKey)?.contentOrNull
                        ?: index.toString() //In case index is non-string, the backward transformation will be broken.

                    val token = key.withIndex(indexValue)
                    map[token] = jsonElement.toMetaItem(itemDescriptor)
                }
            }
        }
        return map
    }

    override val items: Map<NameToken, MetaItem<JsonMeta>> by lazy(::buildItems)

    companion object {
        /**
         * A key representing top-level json array of nodes, which could not be directly represented by a meta node
         */
        const val JSON_ARRAY_KEY = "@jsonArray"
    }
}