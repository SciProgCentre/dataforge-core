@file:Suppress("UNUSED_PARAMETER")

package space.kscience.dataforge.meta

import kotlinx.serialization.json.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.values.*


/**
 * @param descriptor reserved for custom serialization in future
 */
public fun Value.toJson(descriptor: MetaDescriptor? = null): JsonElement = when (type) {
    ValueType.NUMBER -> JsonPrimitive(numberOrNull)
    ValueType.STRING -> JsonPrimitive(string)
    ValueType.BOOLEAN -> JsonPrimitive(boolean)
    ValueType.LIST -> JsonArray(list.map { it.toJson() })
    ValueType.NULL -> JsonNull
}

//Use these methods to customize JSON key mapping
@Suppress("NULLABLE_EXTENSION_OPERATOR_WITH_SAFE_CALL_RECEIVER")
private fun String.toJsonKey(descriptor: MetaDescriptor?) = descriptor?.attributes?.get("jsonName").string ?: toString()

private fun Meta.toJsonWithIndex(descriptor: MetaDescriptor?, index: String?): JsonElement = if (items.isEmpty()) {
    value?.toJson(descriptor) ?: JsonNull
} else {
    val pairs: MutableList<Pair<String, JsonElement>> = items.entries.groupBy {
        it.key.body
    }.mapTo(ArrayList()) { (body, list) ->
        val childDescriptor = descriptor?.children?.get(body)
        if (list.size == 1) {
            val (token, element) = list.first()
            val child: JsonElement = element.toJsonWithIndex(childDescriptor, token.index)
            body to child
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
        pairs += Meta.INDEX_KEY to JsonPrimitive(index)
    }

    //Add value if needed
    if (value != null) {
        pairs += Meta.VALUE_KEY to value!!.toJson(null)
    }

    JsonObject(pairs.toMap())
}

public fun Meta.toJson(descriptor: MetaDescriptor? = null): JsonElement = toJsonWithIndex(descriptor, null)

public fun JsonObject.toMeta(descriptor: MetaDescriptor? = null): JsonMeta = JsonMeta(this, descriptor)

public fun JsonPrimitive.toValue(descriptor: MetaDescriptor?): Value {
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

public fun JsonElement.toMeta(descriptor: MetaDescriptor? = null): TypedMeta<JsonMeta> = JsonMeta(this, descriptor)

/**
 * A meta wrapping json object
 */
public class JsonMeta(
    private val json: JsonElement,
    private val descriptor: MetaDescriptor? = null
) : TypedMeta<JsonMeta> {

    private val indexName by lazy { descriptor?.indexKey ?: Meta.INDEX_KEY }

    override val value: Value? by lazy {
        when (json) {
            is JsonPrimitive -> json.toValue(descriptor)
            is JsonObject -> json[Meta.VALUE_KEY]?.let { JsonMeta(it).value }
            is JsonArray -> if (json.all { it is JsonPrimitive }) {
                //convert array of primitives to ListValue
                json.map { (it as JsonPrimitive).toValue(descriptor) }.asValue()
            } else {
                null
            }
        }
    }

    override val items: Map<NameToken, JsonMeta> by lazy {
        when (json) {
            is JsonPrimitive -> emptyMap()
            is JsonObject -> json.entries.associate { (name, child) ->
                val index = (child as? JsonObject)?.get(indexName)?.jsonPrimitive?.content
                val token = NameToken(name, index)
                token to JsonMeta(child, descriptor?.children?.get(name))
            }
            is JsonArray -> json.mapIndexed { index, child ->
                //Use explicit index or or order for index
                val tokenIndex = (child as? JsonObject)?.get(indexName)?.jsonPrimitive?.content ?: index.toString()
                val token = NameToken(JSON_ARRAY_KEY, tokenIndex)
                token to JsonMeta(child)
            }.toMap()
        }
    }

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)

    public companion object {
        /**
         * A key representing top-level json array of nodes, which could not be directly represented by a meta node
         */
        public const val JSON_ARRAY_KEY: String = "@jsonArray"
    }
}