package hep.dataforge.io

import hep.dataforge.context.Named
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.io.MetaFormat.Companion.META_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.provider.Type
import kotlinx.io.core.*

/**
 * A format for meta serialization
 */
@Type(META_FORMAT_TYPE)
interface MetaFormat : IOFormat<Meta>, Named {
    override val name: String
    val key: Short

    override fun Output.writeThis(obj: Meta) {
        writeMeta(obj, null)
    }

    override fun Input.readThis(): Meta = readMeta(null)

    fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor? = null)
    fun Input.readMeta(descriptor: NodeDescriptor? = null): Meta

    companion object{
        const val META_FORMAT_TYPE = "metaFormat"
    }
}

fun Meta.toString(format: MetaFormat = JsonMetaFormat): String = buildPacket {
    format.run { writeThis(this@toString) }
}.readText()

fun Meta.toBytes(format: MetaFormat = JsonMetaFormat): ByteReadPacket = buildPacket {
    format.run { writeThis(this@toBytes) }
}


fun MetaFormat.parse(str: String): Meta {
    return ByteReadPacket(str.toByteArray()).readThis()
}

fun MetaFormat.fromBytes(packet: ByteReadPacket): Meta {
    return packet.readThis()
}


