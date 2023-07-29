@file:Suppress("UNUSED_PARAMETER")

package space.kscience.dataforge.meta

import kotlinx.serialization.json.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.names.NameToken

private const val jsonArrayKey: String = "@jsonArray"

public val Meta.Companion.JSON_ARRAY_KEY: String get() = jsonArrayKey

/**
 * @param descriptor reserved for custom serialization in future
 */
public fun Value.toJson(descriptor: MetaDescriptor? = null): JsonElement = when (type) {
    ValueType.NUMBER -> JsonPrimitive(numberOrNull)
    ValueType.STRING -> JsonPrimitive(string)
    ValueType.BOOLEAN -> JsonPrimitive(boolean)
    ValueType.LIST -> JsonArray(list.map { it.toJson(descriptor) })
    ValueType.NULL -> JsonNull
}

//Use these methods to customize JSON key mapping
private fun String.toJsonKey(descriptor: MetaDescriptor?) = descriptor?.attributes?.get("jsonName").string ?: toString()

private fun Meta.toJsonWithIndex(descriptor: MetaDescriptor?, index: String?): JsonElement = if (items.isEmpty()) {
    value?.toJson(descriptor) ?: JsonObject(emptyMap())
} else {
    val pairs: MutableList<Pair<String, JsonElement>> = items.entries.groupBy {
        it.key.body
    }.mapTo(ArrayList()) { (body, list) ->
        val childDescriptor = descriptor?.children?.get(body)
        if (list.size == 1) {
            val (token, element) = list.first()
                //do not add an empty element
                val child: JsonElement = element.toJsonWithIndex(childDescriptor, token.index)
            if(token.index == null) {
                body to child
            } else {
                body to JsonArray(listOf(child))
            }
        } else {
            val elements: List<JsonElement> = list.sortedBy { it.key.index }.mapIndexed { index, entry ->
                //Use index if it is not equal to the item order
                val actualIndex = if (index.toString() != entry.key.index) entry.key.index else null
                entry.value.toJsonWithIndex(childDescriptor, actualIndex)
            }
            body to JsonArray(elements)
        }
    }

    //Add index if needed
    if (index != null) {
        pairs += (descriptor?.indexKey ?: Meta.INDEX_KEY) to JsonPrimitive(index)
    }

    //Add value if needed
    if (value != null) {
        pairs += Meta.VALUE_KEY to value!!.toJson(descriptor)
    }

    JsonObject(pairs.toMap())
}

/**
 * Convert Meta to [JsonElement]. Meta with children is converted to [JsonObject].
 * Meta without children is converted to either [JsonPrimitive] or [JsonArray] depending on the value type.
 * An empty Meta is converted to an empty JsonObject.
 */
public fun Meta.toJson(descriptor: MetaDescriptor? = null): JsonElement = toJsonWithIndex(descriptor, null)

/**
 * Convert a Json primitive to a [Value]
 */
public fun JsonPrimitive.toValue(descriptor: MetaDescriptor?): Value = when (this) {
    JsonNull -> Null
    else -> {
        if (isString) {
            content.asValue()
        } else {
            //consider using LazyParse
            content.parseValue()
        }
    }
}

/**
 * Turn this [JsonElement] into a [ListValue] with recursion or return null if it contains objects
 */
private fun JsonElement.toValueOrNull(descriptor: MetaDescriptor?): Value? = when (this) {
    is JsonPrimitive -> toValue(descriptor)
    is JsonObject -> get(Meta.VALUE_KEY)?.toValueOrNull(descriptor)
    is JsonArray -> {
        if (isEmpty()) ListValue.EMPTY else {
            val values = map { it.toValueOrNull(descriptor) }
            values.map { it ?: return null }.asValue()
        }
    }
}

/**
 * Fill a mutable map with children produced from [element] with given top level [key]
 */
private fun MutableMap<NameToken, SealedMeta>.addJsonElement(
    key: String,
    element: JsonElement,
    descriptor: MetaDescriptor?
) {
    when (element) {
        is JsonPrimitive -> put(NameToken(key), Meta(element.toValue(descriptor)))
        is JsonArray -> {
            val value = element.toValueOrNull(descriptor)
            if (value != null) {
                put(NameToken(key), Meta(value))
            } else {
                val indexKey = descriptor?.indexKey ?: Meta.INDEX_KEY
                element.forEachIndexed { serial, childElement ->
                    val index = (childElement as? JsonObject)?.get(indexKey)?.jsonPrimitive?.content
                        ?: serial.toString()
                    val child: SealedMeta = when (childElement) {
                        is JsonObject -> childElement.toMeta(descriptor)
                        is JsonArray -> {
                            val childValue = childElement.toValueOrNull(null)
                            if (childValue == null) {
                                SealedMeta(null,
                                    hashMapOf<NameToken, SealedMeta>().apply {
                                        addJsonElement(Meta.JSON_ARRAY_KEY, childElement, null)
                                    }
                                )
                            } else {
                                Meta(childValue)
                            }
                        }
                        is JsonPrimitive -> Meta(childElement.toValue(null))
                    }
                    put(NameToken(key, index), child)
                }
            }
        }
        is JsonObject -> {
            val indexKey = descriptor?.indexKey ?: Meta.INDEX_KEY
            val index = element[indexKey]?.jsonPrimitive?.content
            put(NameToken(key, index), element.toMeta(descriptor))
        }
    }
}

public fun JsonObject.toMeta(descriptor: MetaDescriptor? = null): SealedMeta {
    val map = LinkedHashMap<NameToken, SealedMeta>()
    forEach { (key, element) ->
        if (key != Meta.VALUE_KEY) {
            map.addJsonElement(key, element, descriptor?.get(key))
        }
    }
    return SealedMeta(get(Meta.VALUE_KEY)?.toValueOrNull(descriptor), map)
}

public fun JsonElement.toMeta(descriptor: MetaDescriptor? = null): SealedMeta = when (this) {
    is JsonPrimitive -> Meta(toValue(descriptor))
    is JsonObject -> toMeta(descriptor)
    is JsonArray -> SealedMeta(null,
        linkedMapOf<NameToken, SealedMeta>().apply {
            addJsonElement(Meta.JSON_ARRAY_KEY, this@toMeta, null)
        }
    )
}

//
///**
// * A meta wrapping json object
// */
//public class JsonMeta(
//    private val json: JsonElement,
//    private val descriptor: MetaDescriptor? = null
//) : TypedMeta<JsonMeta> {
//
//    private val indexName by lazy { descriptor?.indexKey ?: Meta.INDEX_KEY }
//
//    override val value: Value? by lazy {
//        json.toValueOrNull(descriptor)
//    }
//
//    private fun MutableMap<NameToken, JsonMeta>.appendArray(json: JsonArray, key: String) {
//        json.forEachIndexed { index, child ->
//            if (child is JsonArray) {
//                appendArray(child, key)
//            } else {
//                //Use explicit index or order for index
//                val tokenIndex = (child as? JsonObject)
//                    ?.get(indexName)
//                    ?.jsonPrimitive?.content
//                    ?: index.toString()
//                val token = NameToken(key, tokenIndex)
//                this[token] = JsonMeta(child)
//            }
//        }
//    }
//
//    override val items: Map<NameToken, JsonMeta> by lazy {
//        val map = HashMap<NameToken, JsonMeta>()
//        when (json) {
//            is JsonObject -> json.forEach { (name, child) ->
//                //skip value key
//                if (name != Meta.VALUE_KEY) {
//                    if (child is JsonArray && child.any { it is JsonObject }) {
//                        map.appendArray(child, name)
//                    } else {
//
//                        val index = (child as? JsonObject)?.get(indexName)?.jsonPrimitive?.content
//                        val token = NameToken(name, index)
//                        map[token] = JsonMeta(child, descriptor?.get(name))
//                    }
//                }
//            }
//            is JsonArray -> {
//                //return children only if it is not value
//                if (value == null) map.appendArray(json, JSON_ARRAY_KEY)
//            }
//            else -> {
//                //do nothing
//            }
//        }
//        map
//    }
//
//    override fun toString(): String = Meta.toString(this)
//    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
//    override fun hashCode(): Int = Meta.hashCode(this)
//
//    public companion object {
//        /**
//         * A key representing top-level json array of nodes, which could not be directly represented by a meta node
//         */
//        public const val JSON_ARRAY_KEY: String = "@jsonArray"
//    }
//}