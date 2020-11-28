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
public interface MetaFormat : IOFormat<Meta> {

    override fun writeObject(output: Output, obj: Meta) {
        writeMeta(output, obj, null)
    }

    override fun readObject(input: Input): Meta = readMeta(input)

    public fun writeMeta(
        output: Output,
        meta: Meta,
        descriptor: NodeDescriptor? = null,
    )

    public fun readMeta(input: Input, descriptor: NodeDescriptor? = null): Meta
}

@Type(META_FORMAT_TYPE)
public interface MetaFormatFactory : IOFormatFactory<Meta>, MetaFormat {
    public val shortName: String

    override val name: Name get() = "meta".asName() + shortName

    override val type: KClass<out Meta> get() = Meta::class

    public val key: Short get() = name.hashCode().toShort()

    override operator fun invoke(meta: Meta, context: Context): MetaFormat

    public companion object {
        public const val META_FORMAT_TYPE: String = "io.format.meta"
    }
}

public fun Meta.toString(format: MetaFormat): String = buildByteArray {
    format.run { writeObject(this@buildByteArray, this@toString) }
}.decodeToString()

public fun Meta.toString(formatFactory: MetaFormatFactory): String = toString(formatFactory())

public fun MetaFormat.parse(str: String): Meta {
    return ByteArrayInput(str.encodeToByteArray()).use { readObject(it) }
}

public fun MetaFormatFactory.parse(str: String, formatMeta: Meta): Meta = invoke(formatMeta).parse(str)


