@file:Suppress("UNUSED_PARAMETER")

package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.descriptors.ValueDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBase
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.serialization.toJson
import hep.dataforge.meta.serialization.toMeta
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName
import hep.dataforge.values.*
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.text.readUtf8String
import kotlinx.io.text.writeUtf8String
import kotlinx.serialization.UnstableDefault


import kotlinx.serialization.json.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@OptIn(UnstableDefault::class)
class JsonMetaFormat(private val json: Json = DEFAULT_JSON) : MetaFormat {

    override fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?) {
        val jsonObject = meta.toJson(descriptor)
        writeUtf8String(json.stringify(JsonObjectSerializer, jsonObject))
    }

    override fun Input.readMeta(descriptor: NodeDescriptor?): Meta {
        val str = readUtf8String()
        val jsonElement = json.parseJson(str)
        return jsonElement.toMeta()
    }

    companion object : MetaFormatFactory {
        val DEFAULT_JSON = Json{prettyPrint = true}

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
