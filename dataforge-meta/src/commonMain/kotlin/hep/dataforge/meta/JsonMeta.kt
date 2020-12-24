@file:Suppress("UNUSED_PARAMETER")

package hep.dataforge.meta

import hep.dataforge.meta.JsonMeta.Companion.JSON_ARRAY_KEY
import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.meta.descriptors.ItemDescriptor.Companion.DEFAULT_INDEX_KEY
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.descriptors.ValueDescriptor
import hep.dataforge.names.NameToken
import hep.dataforge.names.withIndex
import hep.dataforge.values.*
import kotlinx.serialization.json.*


/**
 * @param descriptor reserved for custom serialization in future
 */
public fun Value.toJson(descriptor: ValueDescriptor? = null): JsonElement = when (type) {
    ValueType.NUMBER -> JsonPrimitive(numberOrNull)
    ValueType.STRING -> JsonPrimitive(string)
    ValueType.BOOLEAN -> JsonPrimitive(boolean)
    ValueType.LIST -> JsonArray(list.map { it.toJson() })
    ValueType.NULL -> JsonNull
}

//Use these methods to customize JSON key mapping
@Suppress("NULLABLE_EXTENSION_OPERATOR_WITH_SAFE_CALL_RECEIVER")
private fun String.toJsonKey(descriptor: ItemDescriptor?) = descriptor?.attributes?.get("jsonName").string ?: toString()

//private fun NodeDescriptor?.getDescriptor(key: String) = this?.items?.get(key)

/**
 * Convert given [Meta] to [JsonObject]. Primitives and nodes are copied as is, same name siblings are treated as json arrays
 */
private fun Meta.toJsonWithIndex(descriptor: NodeDescriptor?, indexValue: String?): JsonObject {

    val elementMap = HashMap<String, JsonElement>()

    fun MetaItem.toJsonElement(itemDescriptor: ItemDescriptor?, index: String?): JsonElement = when (this) {
        is ValueItem -> {
            value.toJson(itemDescriptor as? ValueDescriptor)
        }
        is NodeItem -> {
            node.toJsonWithIndex(itemDescriptor as? NodeDescriptor, index)
        }
    }

    fun addElement(key: String) {
        val itemDescriptor = descriptor?.items?.get(key)
        val jsonKey = key.toJsonKey(itemDescriptor)
        val items: Map<String?, MetaItem> = getIndexed(key)
        when (items.size) {
            0 -> {
                //do nothing
            }
            1 -> {
                elementMap[jsonKey] = items.values.first().toJsonElement(itemDescriptor, null)
            }
            else -> {
                val array = buildJsonArray {
                    items.forEach { (index, item) ->
                        add(item.toJsonElement(itemDescriptor, index))
                    }
                }
                elementMap[jsonKey] = array
            }
        }
    }

    ((descriptor?.items?.keys ?: emptySet()) + items.keys.map { it.body }).forEach(::addElement)


    if (indexValue != null) {
        val indexKey = descriptor?.indexKey ?: DEFAULT_INDEX_KEY
        elementMap[indexKey] = JsonPrimitive(indexValue)
    }

    return JsonObject(elementMap)
}

public fun Meta.toJson(descriptor: NodeDescriptor? = null): JsonObject = toJsonWithIndex(descriptor, null)

public fun JsonObject.toMeta(descriptor: NodeDescriptor? = null): JsonMeta = JsonMeta(this, descriptor)

public fun JsonPrimitive.toValue(descriptor: ValueDescriptor?): Value {
    return when (this) {
        JsonNull -> Null
        else -> {
            if (isString) {
                StringValue(content)
            } else {
                content.parseValue()
            }
        }
    }
}

public fun JsonElement.toMetaItem(descriptor: ItemDescriptor? = null): TypedMetaItem<JsonMeta> = when (this) {
    is JsonPrimitive -> {
        val value = this.toValue(descriptor as? ValueDescriptor)
        ValueItem(value)
    }
    is JsonObject -> {
        val meta = JsonMeta(this, descriptor as? NodeDescriptor)
        NodeItem(meta)
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
            ValueItem(value)
        } else {
            //We can't return multiple items therefore we create top level node
            buildJsonObject { put(JSON_ARRAY_KEY, this@toMetaItem) }.toMetaItem(descriptor)
        }
    }
}

/**
 * A meta wrapping json object
 */
public class JsonMeta(private val json: JsonObject, private val descriptor: NodeDescriptor? = null) : MetaBase() {

    private fun buildItems(): Map<NameToken, TypedMetaItem<JsonMeta>> {
        val map = LinkedHashMap<NameToken, TypedMetaItem<JsonMeta>>()

        json.forEach { (jsonKey, value) ->
            val key = NameToken(jsonKey)
            val itemDescriptor = descriptor?.items?.get(jsonKey)
            when (value) {
                is JsonPrimitive -> {
                    map[key] = ValueItem(value.toValue(itemDescriptor as? ValueDescriptor))
                }
                is JsonObject -> {
                    map[key] = NodeItem(
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
                    map[key] = ValueItem(listValue)
                } else value.forEachIndexed { index, jsonElement ->
                    val indexKey = (itemDescriptor as? NodeDescriptor)?.indexKey ?: DEFAULT_INDEX_KEY
                    val indexValue: String = (jsonElement as? JsonObject)
                        ?.get(indexKey)?.jsonPrimitive?.contentOrNull
                        ?: index.toString() //In case index is non-string, the backward transformation will be broken.

                    val token = key.withIndex(indexValue)
                    map[token] = jsonElement.toMetaItem(itemDescriptor)
                }
            }
        }
        return map
    }

    override val items: Map<NameToken, TypedMetaItem<JsonMeta>> by lazy(::buildItems)

    public companion object {
        /**
         * A key representing top-level json array of nodes, which could not be directly represented by a meta node
         */
        public const val JSON_ARRAY_KEY: String = "@jsonArray"
    }
}