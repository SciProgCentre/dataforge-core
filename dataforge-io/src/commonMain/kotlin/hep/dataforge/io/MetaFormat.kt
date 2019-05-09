package hep.dataforge.io

import hep.dataforge.meta.Meta
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.toByteArray

/**
 * A format for meta serialization
 */
interface MetaFormat: IOFormat<Meta>

/**
 * ServiceLoader compatible factory
 */
interface MetaFormatFactory {
    val name: String
    val key: Short

    fun build(): MetaFormat
}

fun Meta.asString(format: MetaFormat = JsonMetaFormat): String {
    val builder = BytePacketBuilder()
    format.write(this, builder)
    return builder.build().readText()
}

fun MetaFormat.parse(str: String): Meta {
    return read(ByteReadPacket(str.toByteArray()))
}


