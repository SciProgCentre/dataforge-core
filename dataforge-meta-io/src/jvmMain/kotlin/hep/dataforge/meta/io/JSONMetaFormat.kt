package hep.dataforge.meta.io

import com.github.cliftonlabs.json_simple.JsonArray
import com.github.cliftonlabs.json_simple.JsonObject
import com.github.cliftonlabs.json_simple.Jsoner
import hep.dataforge.meta.*
import hep.dataforge.names.toName
import hep.dataforge.values.*
import kotlinx.io.core.*
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.ByteBuffer
import java.text.ParseException

internal actual fun writeJson(meta: Meta, out: Output) {
    val json = meta.toJson()
    val string = Jsoner.prettyPrint(Jsoner.serialize(json))
    out.writeText(string)
}

private fun Value.toJson(): Any {
    return if (list.size == 1) {
        when (type) {
            ValueType.NUMBER -> number
            ValueType.BOOLEAN -> boolean
            else -> string
        }
    } else {
        JsonArray().apply {
            list.forEach { add(it.toJson()) }
        }
    }
}

fun Meta.toJson(): JsonObject {
    val builder = JsonObject()
    items.forEach { name, item ->
        when (item) {
            is MetaItem.ValueItem -> builder[name.toString()] = item.value.toJson()
            is MetaItem.NodeItem -> builder[name.toString()] = item.node.toJson()
        }
    }
    return builder
}


internal actual fun readJson(input: Input, length: Int): Meta {
    return if (length == 0) {
        EmptyMeta
    } else {
        val json = if (length > 0) {
            //Read into intermediate buffer
            val buffer = ByteArray(length)
            input.readAvailable(buffer, length)
            Jsoner.deserialize(InputStreamReader(ByteArrayInputStream(buffer), Charsets.UTF_8)) as JsonObject
        } else {
            //automatic
            val reader = object : Reader() {
                override fun close() {
                    input.close()
                }

                override fun read(cbuf: CharArray, off: Int, len: Int): Int {
                    val buffer = ByteBuffer.allocate(len)
                    val res = input.readAvailable(buffer)
                    val chars = String(buffer.array()).toCharArray()
                    System.arraycopy(chars, 0, cbuf, off, chars.size)
                    return res
                }

            }
            Jsoner.deserialize(reader) as JsonObject
        }
        json.toMeta()
    }
}

@Throws(ParseException::class)
private fun JsonObject.toMeta(): Meta {
    return buildMeta {
        this@toMeta.forEach { key, value -> appendValue(key as String, value) }
    }
}

private fun JsonArray.toListValue(): Value {
    val list: List<Value> = this.map { value ->
        when (value) {
            null -> Null
            is JsonArray -> value.toListValue()
            is Number -> NumberValue(value)
            is Boolean -> if (value) True else False
            is String -> LazyParsedValue(value)
            is JsonObject -> error("Object values inside multidimensional arrays are not allowed")
            else -> error("Unknown token $value in json")
        }
    }
    return Value.of(list)
}

private fun MetaBuilder.appendValue(key: String, value: Any?) {
    when (value) {
        is JsonObject -> this[key] = value.toMeta()
        is JsonArray -> {
            if (value.none { it is JsonObject }) {
                //If all values are primitives or arrays
                this[key] = value.toListValue()
            } else {
                val list = value.map<Any, Meta> {
                    when (it) {
                        is JsonObject -> it.toMeta()
                        is JsonArray -> it.toListValue().toMeta()
                        else -> Value.of(it).toMeta()
                    }
                }
                setIndexed(key.toName(), list)
            }
        }
        is Number -> this[key] = NumberValue(value)
        is Boolean -> this[key] = value
        is String -> this[key] = LazyParsedValue(value)
        //ignore anything else
    }
}
