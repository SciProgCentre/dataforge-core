package space.kscience.dataforge.io


import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DfType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A format for meta serialization
 */
public interface MetaFormat : IOFormat<Meta> {

    override val type: KType get() = typeOf<Meta>()

    override fun writeTo(sink: Sink, obj: Meta) {
        writeMeta(sink, obj, null)
    }

    override fun readFrom(source: Source): Meta = readMeta(source)

    public fun writeMeta(
        sink: Sink,
        meta: Meta,
        descriptor: MetaDescriptor? = null,
    )

    public fun readMeta(source: Source, descriptor: MetaDescriptor? = null): Meta
}

@DfType(META_FORMAT_TYPE)
public interface MetaFormatFactory : IOFormatFactory<Meta>, MetaFormat {
    public val shortName: String

    override val name: Name get() = "meta".asName() + shortName

    override val type: KType get() = typeOf<Meta>()

    public val key: Short get() = name.hashCode().toShort()

    override fun build(context: Context, meta: Meta): MetaFormat

    public companion object {
        public const val META_FORMAT_TYPE: String = "io.format.meta"
    }
}

public fun Meta.toString(format: MetaFormat): String = ByteArray {
    format.run {
        writeTo(this@ByteArray, this@toString)
    }
}.decodeToString()

public fun Meta.toString(formatFactory: MetaFormatFactory): String = toString(formatFactory.build(Global, Meta.EMPTY))

public fun MetaFormat.parse(str: String): Meta = readFrom(StringSource(str).buffered())

public fun MetaFormatFactory.parse(str: String, formatMeta: Meta): Meta = build(Global, formatMeta).parse(str)


