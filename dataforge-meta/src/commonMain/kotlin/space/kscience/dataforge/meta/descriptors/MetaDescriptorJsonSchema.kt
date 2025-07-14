package space.kscience.dataforge.meta.descriptors

import kotlinx.serialization.json.*
import space.kscience.dataforge.meta.*

/**
 * A converter between [MetaDescriptor] and JSON Schema ([JsonObject]) representations.
 *
 * Provides bidirectional conversion between metadata descriptors and JSON Schema format,
 * handling:
 * - Basic schema metadata (title, description)
 * - Value type restrictions and validation
 * - Allowed values (enums)
 * - Required fields
 * - Nested properties
 * - Default values
 * - Custom metadata fields (indexKey, multiple, attributes)
 *
 * The converter maintains JSON Schema compatibility while preserving all [MetaDescriptor] features.
 */
private object MetaDescriptorJsonSchemaConverter {
    fun convertMetaDescriptorToJsonSchema(metaDescriptor: MetaDescriptor): JsonObject {
        return convertMetaDescriptorToJsonSchema(metaDescriptor, 1, null)
    }

    private fun convertMetaDescriptorToJsonSchema(metaDescriptor: MetaDescriptor, depth: Int, title: String?): JsonObject = buildJsonObject {
        // Basic metadata
        if (depth == 1) {
            put(JsonSchema.Vocabularies.Core.SCHEMA, JsonSchema.VERSION.value)
        }

        title?.let { put(JsonSchema.Vocabularies.MetaData.TITLE, it) }
        metaDescriptor.description?.let { put(JsonSchema.Vocabularies.MetaData.DESCRIPTION, it) }

        // Value type handling
        when (metaDescriptor.valueRestriction) {
            ValueRestriction.ABSENT -> put(JsonSchema.Vocabularies.Validation.TYPE, JsonPrimitive("null"))
            else -> {
                metaDescriptor.valueTypes?.let { types ->
                    put(
                        JsonSchema.Vocabularies.Validation.TYPE,
                        if (types.size == 1) JsonPrimitive(valueTypeToJsonType(types[0]))
                        else buildJsonArray { types.mapToJsonTypes().forEach(::add) }
                    )
                }
            }
        }

        // Handle allowed values
        metaDescriptor.allowedValues?.let { allowed ->
            put(JsonSchema.Vocabularies.Validation.ENUM, buildJsonArray {
                allowed.map { it.mapToJsonElement() }.forEach(::add)
            })
        }

        // Handle required fields
        val listOfRequired = metaDescriptor.nodes.filter { (_, node) -> node.required }.map { (name, _) -> name }
        if (!listOfRequired.isEmpty()) {
            put(JsonSchema.Vocabularies.Validation.REQUIRED, buildJsonArray { listOfRequired.forEach(::add) })
        }

        // Handle child nodes
        if (metaDescriptor.nodes.isNotEmpty()) {
            put(
                JsonSchema.Vocabularies.Applicator.PROPERTIES, buildJsonObject {
                    metaDescriptor.nodes.forEach { (title, node) ->
                        put(title, convertMetaDescriptorToJsonSchema(node, depth + 1, title))
                    }
                })
        }

        // Handle default value
        if (metaDescriptor.defaultValue != null) {
            put(JsonSchema.Vocabularies.MetaData.DEFAULT, metaDescriptor.defaultValue.mapToJsonElement())
        }

        // Custom meta descriptor fields
        put(JsonSchema.Vocabularies.Custom.INDEX_KEY, metaDescriptor.indexKey)
        put(JsonSchema.Vocabularies.Custom.MULTIPLE, metaDescriptor.multiple)
        put(JsonSchema.Vocabularies.Custom.ATTRIBUTES, Json.encodeToJsonElement(metaDescriptor.attributes))
    }

    fun convertJsonSchemaToMetaDescriptor(jsonObject: JsonObject): MetaDescriptor {
        return convertJsonSchemaToMetaDescriptor(jsonObject, 1)
    }

    private fun convertJsonSchemaToMetaDescriptor(jsonObject: JsonObject, depth: Int): MetaDescriptor {
        val builder = MetaDescriptorBuilder()

        // Handle basic metadata
        builder.description = jsonObject[JsonSchema.Vocabularies.MetaData.DESCRIPTION]?.jsonPrimitive?.contentOrNull

        // Handle value types
        val typeElement = jsonObject[JsonSchema.Vocabularies.Validation.TYPE]
        val valueTypes = when (typeElement) {
            is JsonPrimitive -> listOfNotNull(jsonTypeToValueType(typeElement.contentOrNull))
            is JsonArray -> typeElement.mapNotNull { jsonTypeToValueType(it.jsonPrimitive.contentOrNull) }
            else -> null
        }
        builder.valueTypes = valueTypes?.takeIf { it.isNotEmpty() }

        // Handle value restriction
        builder.valueRestriction = when {
            valueTypes?.contains(ValueType.NULL) == true && valueTypes.size == 1 -> ValueRestriction.ABSENT
            jsonObject[JsonSchema.Vocabularies.Validation.REQUIRED] != null -> ValueRestriction.REQUIRED
            else -> ValueRestriction.NONE
        }

        // Handle allowed values
        jsonObject[JsonSchema.Vocabularies.Validation.ENUM]?.jsonArray?.let { enumArray ->
            builder.allowedValues = enumArray.map { jsonElement ->
                when (jsonElement) {
                    is JsonPrimitive -> when {
                        jsonElement.isString -> StringValue(jsonElement.content)
                        jsonElement.booleanOrNull != null -> if (jsonElement.boolean) True else False
                        jsonElement.doubleOrNull != null -> NumberValue(jsonElement.double)
                        else -> Null
                    }
                    is JsonArray -> ListValue(jsonElement.map { it.mapToValue() })
                    else -> Null
                }
            }
        }

        // Handle default value
        jsonObject[JsonSchema.Vocabularies.MetaData.DEFAULT]?.let { defaultValue ->
            builder.default = defaultValue.mapToValue()
        }

        // Handle child nodes
        jsonObject[JsonSchema.Vocabularies.Applicator.PROPERTIES]?.jsonObject?.let { properties ->
            properties.forEach { (name, schema) ->
                builder.node(name, convertJsonSchemaToMetaDescriptor(schema.jsonObject, depth + 1))
            }
        }

        // Handle required fields
        val requiredFields = jsonObject[JsonSchema.Vocabularies.Validation.REQUIRED]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        if (requiredFields.isNotEmpty()) {
            builder.children.forEach { (name, childBuilder) ->
                if (name in requiredFields) {
                    childBuilder.valueRestriction = ValueRestriction.REQUIRED
                }
            }
        }

        // Handle custom fields
        builder.indexKey = jsonObject[JsonSchema.Vocabularies.Custom.INDEX_KEY]?.jsonPrimitive?.contentOrNull ?: Meta.INDEX_KEY
        builder.multiple = jsonObject[JsonSchema.Vocabularies.Custom.MULTIPLE]?.jsonPrimitive?.booleanOrNull ?: false
        jsonObject[JsonSchema.Vocabularies.Custom.ATTRIBUTES]?.jsonObject?.let { attributes ->
            builder.attributes.update(Json.decodeFromJsonElement(attributes))
        }

        return builder.build()
    }

    /**
     * Convert [ValueType] to JSON Schema type string
     */
    private fun valueTypeToJsonType(valueType: ValueType): String = when (valueType) {
        ValueType.NUMBER -> "number"
        ValueType.STRING -> "string"
        ValueType.BOOLEAN -> "boolean"
        ValueType.LIST -> "array"
        ValueType.NULL -> "null"
    }

    /**
     * Convert JSON Schema type string to [ValueType]
     */
    private fun jsonTypeToValueType(type: String?): ValueType? = when (type) {
        "number" -> ValueType.NUMBER
        "string" -> ValueType.STRING
        "boolean" -> ValueType.BOOLEAN
        "array" -> ValueType.LIST
        "null" -> ValueType.NULL
        else -> null
    }

    private fun List<ValueType>.mapToJsonTypes() = map { valueTypeToJsonType(it) }

    private fun Value.mapToJsonElement(): JsonElement = when (type) {
        ValueType.NUMBER -> JsonPrimitive(number)
        ValueType.STRING -> JsonPrimitive(string)
        ValueType.BOOLEAN -> JsonPrimitive(boolean)
        ValueType.LIST -> buildJsonArray { list.map { it.mapToJsonElement() }.forEach(::add) }
        ValueType.NULL -> JsonNull
    }

    private fun JsonElement.mapToValue(): Value = when (this) {
        is JsonPrimitive -> when {
            isString -> StringValue(content)
            booleanOrNull != null -> if (boolean) True else False
            doubleOrNull != null -> NumberValue(double)
            intOrNull != null -> NumberValue(int)
            longOrNull != null -> NumberValue(long)
            else -> Null
        }
        is JsonArray -> ListValue(map { it.mapToValue() })
        is JsonObject -> {
            val meta = MutableMeta()
            forEach { (key, value) ->
                meta[key] = value.mapToValue()
            }
            meta
        }
        JsonNull -> Null
    } as Value
}

/**
 * Convert [MetaDescriptor] to a JSON Schema [JsonObject]
 */
public fun MetaDescriptor.toJsonSchema(): JsonObject {
    return MetaDescriptorJsonSchemaConverter.convertMetaDescriptorToJsonSchema(this)
}

/**
 * Convert JSON Schema [JsonObject] to [MetaDescriptor]
 */
public fun JsonObject.toMetaDescriptor(): MetaDescriptor {
    return MetaDescriptorJsonSchemaConverter.convertJsonSchemaToMetaDescriptor(this)
}
