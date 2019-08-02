package hep.dataforge.io

import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.meta.Meta
import kotlinx.io.core.*

/**
 * A format for meta serialization
 */
interface MetaFormat : IOFormat<Meta> {
    val name: String
    val key: Short

    override fun Output.writeObject(obj: Meta) {
        writeMeta(obj, null)
    }

    override fun Input.readObject(): Meta = readMeta(null)

    fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor?)
    fun Input.readMeta(descriptor: NodeDescriptor?): Meta
}

fun Meta.toString(format: MetaFormat = JsonMetaFormat): String = buildPacket {
    format.run { writeObject(this@toString) }
}.readText()

fun Meta.toBytes(format: MetaFormat = JsonMetaFormat): ByteReadPacket = buildPacket {
    format.run { writeObject(this@toBytes) }
}


fun MetaFormat.parse(str: String): Meta {
    return ByteReadPacket(str.toByteArray()).readObject()
}

fun MetaFormat.fromBytes(packet: ByteReadPacket): Meta {
    return packet.readObject()
}


