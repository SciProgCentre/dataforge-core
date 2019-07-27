package hep.dataforge.io

import hep.dataforge.meta.Meta
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.toByteArray

/**
 * A format for meta serialization
 */
interface MetaFormat : IOFormat<Meta> {
    val name: String
    val key: Short
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


