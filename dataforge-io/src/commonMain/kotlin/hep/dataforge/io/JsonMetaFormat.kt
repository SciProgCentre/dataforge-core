@file:Suppress("UNUSED_PARAMETER")

package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.descriptors.ItemDescriptor
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.descriptors.ValueDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBase
import hep.dataforge.meta.MetaItem
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.plus
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


class JsonMetaFormat(private val json: Json = Json.indented) : MetaFormat {

    override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) {
        val jsonObject = meta.toJson(descriptor)
        writeText(json.stringify(JsonObjectSerializer, jsonObject))
    }

    override fun Input.readMeta(descriptor: NodeDescriptor?): Meta {
        val str = readText()
        val jsonElement = json.parseJson(str)
        return jsonElement.toMeta()
    }

    companion object : MetaFormatFactory {
        val default = JsonMetaFormat()

        override fun invoke(meta: Meta, context: Context): MetaFormat = default

        override val name: Name = super.name + "json"
        override val key: Short = 0x4a53//"JS"
    }
}

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

private fun NodeDescriptor?.getDescriptor(key: String) = this?.items?.get(key)

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

fun JsonElement.toMeta(descriptor: NodeDescriptor? = null): Meta {
    return when (val item = toMetaItem(descriptor)) {
        is MetaItem.NodeItem<*> -> item.node
        is MetaItem.ValueItem ->item.value.toMeta()
    }
}

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
        val itemDescriptor = descriptor.getDescriptor(key)
        //use name from descriptor in case descriptor name differs from json key
        val name = itemDescriptor?.name ?: key
        return when (value) {
            is JsonPrimitive -> {
                this[name] = MetaItem.ValueItem(value.toValue(itemDescriptor as? ValueDescriptor)) as MetaItem<JsonMeta>
            }
            is JsonObject -> {
                this[name] = MetaItem.NodeItem(JsonMeta(value, itemDescriptor as? NodeDescriptor))
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
                        this["$name[$index]"] = jsonElement.toMetaItem(itemDescriptor)
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