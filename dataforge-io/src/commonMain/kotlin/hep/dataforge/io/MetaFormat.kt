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

fun Meta.asString(format: MetaFormat = JsonMetaFormat): String {
    return buildPacket {
        format.run { writeObject(this@asString) }
    }.readText()
}

fun MetaFormat.parse(str: String): Meta {
    return ByteReadPacket(str.toByteArray()).readObject()
}


