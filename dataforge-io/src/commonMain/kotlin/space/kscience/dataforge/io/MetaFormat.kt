package space.kscience.dataforge.io

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import io.ktor.utils.io.core.use
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.Type
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

    override fun writeObject(output: Output, obj: Meta) {
        writeMeta(output, obj, null)
    }

    override fun readObject(input: Input): Meta = readMeta(input)

    public fun writeMeta(
        output: Output,
        meta: Meta,
        descriptor: MetaDescriptor? = null,
    )

    public fun readMeta(input: Input, descriptor: MetaDescriptor? = null): Meta
}

@Type(META_FORMAT_TYPE)
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
        writeObject(this@ByteArray, this@toString)
    }
}.decodeToString()

public fun Meta.toString(formatFactory: MetaFormatFactory): String = toString(formatFactory.build(Global, Meta.EMPTY))

public fun MetaFormat.parse(str: String): Meta {
    return ByteReadPacket(str.encodeToByteArray()).use { readObject(it) }
}

public fun MetaFormatFactory.parse(str: String, formatMeta: Meta): Meta = build(Global, formatMeta).parse(str)


