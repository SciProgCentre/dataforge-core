@file:Suppress("UNUSED_PARAMETER")

package hep.dataforge.io


import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.node
import hep.dataforge.meta.toJson
import hep.dataforge.meta.toMetaItem
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.readByteArray
import kotlinx.io.text.writeUtf8String
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectSerializer

@OptIn(UnstableDefault::class)
class JsonMetaFormat(private val json: Json = DEFAULT_JSON) : MetaFormat {

    override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) {
        val jsonObject = meta.toJson(descriptor)
        writeUtf8String(json.stringify(JsonObjectSerializer, jsonObject))
    }

    override fun toMeta(): Meta  = Meta{
        IOPlugin.IO_FORMAT_NAME_KEY put name.toString()
    }

    override fun Input.readMeta(descriptor: NodeDescriptor?): Meta {
        val str = readByteArray().decodeToString()
        val jsonElement = json.parseJson(str)
        val item = jsonElement.toMetaItem(descriptor)
        return item.node ?: Meta.EMPTY
    }

    companion object : MetaFormatFactory {
        val DEFAULT_JSON = Json { prettyPrint = true }

        override fun invoke(meta: Meta, context: Context): MetaFormat = default

        override val shortName = "json"
        override val key: Short = 0x4a53//"JS"

        private val default = JsonMetaFormat()

        override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) =
            default.run { writeMeta(meta, descriptor) }

        override fun Input.readMeta(descriptor: NodeDescriptor?): Meta =
            default.run { readMeta(descriptor) }
    }
}
