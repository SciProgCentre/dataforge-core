package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.provider.Type
import kotlinx.io.core.*
import kotlin.reflect.KClass

/**
 * A format for meta serialization
 */

interface MetaFormat : IOFormat<Meta> {

    override fun Output.writeThis(obj: Meta) {
        writeMeta(obj, null)
    }

    override fun Input.readThis(): Meta = readMeta(null)

    fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor? = null)
    fun Input.readMeta(descriptor: NodeDescriptor? = null): Meta
}

@Type(META_FORMAT_TYPE)
interface MetaFormatFactory : IOFormatFactory<Meta> {
    override val name: Name get() = "meta".asName()

    override val type: KClass<out Meta> get() = Meta::class

    val key: Short

    override operator fun invoke(meta: Meta, context: Context): MetaFormat

    companion object {
        const val META_FORMAT_TYPE = "io.format.meta"
    }
}

fun Meta.toString(format: MetaFormat): String = buildPacket {
    format.run { writeThis(this@toString) }
}.readText()

fun Meta.toString(formatFactory: MetaFormatFactory): String = toString(formatFactory())

fun Meta.toBytes(format: MetaFormat = JsonMetaFormat.default): ByteReadPacket = buildPacket {
    format.run { writeThis(this@toBytes) }
}

fun MetaFormat.parse(str: String): Meta {
    return ByteReadPacket(str.toByteArray()).readThis()
}

fun MetaFormatFactory.parse(str: String): Meta = invoke().parse(str)

fun MetaFormat.fromBytes(packet: ByteReadPacket): Meta {
    return packet.readThis()
}


