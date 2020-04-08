package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.plus
import hep.dataforge.provider.Type
import kotlinx.io.ByteArrayInput
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.use
import kotlin.reflect.KClass

/**
 * A format for meta serialization
 */

interface MetaFormat : IOFormat<Meta> {

    override fun Output.writeObject(obj: Meta) {
        writeMeta(obj, null)
    }

    override fun Input.readObject(): Meta = readMeta()

    fun Output.writeMeta(meta: Meta, descriptor: NodeDescriptor? = null)
    fun Input.readMeta(descriptor: NodeDescriptor? = null): Meta
}

@Type(META_FORMAT_TYPE)
interface MetaFormatFactory : IOFormatFactory<Meta>, MetaFormat {
    val shortName: String

    override val name: Name get() = "meta".asName() + shortName

    override val type: KClass<out Meta> get() = Meta::class

    val key: Short get() = name.hashCode().toShort()

    override operator fun invoke(meta: Meta, context: Context): MetaFormat

    companion object {
        const val META_FORMAT_TYPE = "io.format.meta"
    }
}

fun Meta.toString(format: MetaFormat): String = buildByteArray {
    format.run { writeObject(this@toString) }
}.decodeToString()

fun Meta.toString(formatFactory: MetaFormatFactory): String = toString(formatFactory())

fun MetaFormat.parse(str: String): Meta {
    return ByteArrayInput(str.encodeToByteArray()).use { it.readObject() }
}

fun MetaFormatFactory.parse(str: String, formatMeta: Meta): Meta = invoke(formatMeta).parse(str)


