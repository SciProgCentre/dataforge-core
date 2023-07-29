@file:Suppress("UNUSED_PARAMETER")

package space.kscience.dataforge.io


import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.toJson
import space.kscience.dataforge.meta.toMeta

/**
 * A Json format for Meta representation
 */
public class JsonMetaFormat(private val json: Json = DEFAULT_JSON) : MetaFormat {

    override fun writeMeta(output: Output, meta: Meta, descriptor: MetaDescriptor?) {
        val jsonElement = meta.toJson(descriptor)
        output.writeUtf8String(json.encodeToString(JsonElement.serializer(), jsonElement))
    }

    override fun readMeta(input: Input, descriptor: MetaDescriptor?): Meta {
        val str = input.readUtf8String()//readByteArray().decodeToString()
        val jsonElement = json.parseToJsonElement(str)
        return jsonElement.toMeta(descriptor)
    }

    public companion object : MetaFormatFactory {
        public val DEFAULT_JSON: Json = Json { prettyPrint = true }

        override fun build(context: Context, meta: Meta): MetaFormat = default

        override val shortName: String = "json"
        override val key: Short = 0x4a53//"JS"

        private val default = JsonMetaFormat()

        override fun writeMeta(output: Output, meta: Meta, descriptor: MetaDescriptor?): Unit =
            default.run { writeMeta(output, meta, descriptor) }

        override fun readMeta(input: Input, descriptor: MetaDescriptor?): Meta =
            default.run { readMeta(input, descriptor) }
    }
}
