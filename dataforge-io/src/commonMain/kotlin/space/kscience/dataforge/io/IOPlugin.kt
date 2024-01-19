package space.kscience.dataforge.io

import space.kscience.dataforge.context.*
import space.kscience.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import space.kscience.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import space.kscience.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public class IOPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    public val ioFormatFactories: Collection<IOFormatFactory<*>> by lazy {
        context.gather<IOFormatFactory<*>>(IO_FORMAT_TYPE).values
    }

    @Suppress("UNCHECKED_CAST")
    @DFInternal
    public fun <T> resolveIOFormat(type: KType, meta: Meta): IOFormat<T>? =
        ioFormatFactories.singleOrNull { it.type == type }?.build(context, meta) as? IOFormat<T>

    @OptIn(DFInternal::class)
    public inline fun <reified T> resolveIOFormat(meta: Meta = Meta.EMPTY): IOFormat<T>? =
        resolveIOFormat(typeOf<T>(), meta)


    public val metaFormatFactories: Collection<MetaFormatFactory> by lazy {
        context.gather<MetaFormatFactory>(META_FORMAT_TYPE).values
    }

    public fun resolveMetaFormat(key: Short, meta: Meta = Meta.EMPTY): MetaFormat? =
        metaFormatFactories.find { it.key == key }?.build(context, meta)

    public fun resolveMetaFormat(name: String, meta: Meta = Meta.EMPTY): MetaFormat? =
        metaFormatFactories.find { it.shortName == name }?.build(context, meta)

    public val envelopeFormatFactories: Collection<EnvelopeFormatFactory> by lazy {
        context.gather<EnvelopeFormatFactory>(ENVELOPE_FORMAT_TYPE).values
    }

    private fun resolveEnvelopeFormat(name: Name, meta: Meta = Meta.EMPTY): EnvelopeFormat? =
        envelopeFormatFactories.find { it.name == name }?.build(context, meta)

    public fun resolveEnvelopeFormat(item: Meta): EnvelopeFormat? {
        val name = item.string ?: item[IOFormatFactory.NAME_KEY]?.string ?: error("Envelope format name not defined")
        val meta = item[IOFormatFactory.META_KEY] ?: Meta.EMPTY
        return resolveEnvelopeFormat(Name.parse(name), meta)
    }

    override fun content(target: String): Map<Name, Any> = when (target) {
        META_FORMAT_TYPE -> defaultMetaFormats.associateByName()
        ENVELOPE_FORMAT_TYPE -> defaultEnvelopeFormats.associateByName()
        IO_FORMAT_TYPE -> content(META_FORMAT_TYPE) + content(ENVELOPE_FORMAT_TYPE)
        else -> super.content(target)
    }

    public companion object : PluginFactory<IOPlugin> {
        public val defaultMetaFormats: List<MetaFormatFactory> = listOf(JsonMetaFormat)
        public val defaultEnvelopeFormats: List<EnvelopeFormatFactory> = listOf(
            TaggedEnvelopeFormat,
            TaglessEnvelopeFormat
        )

        override val tag: PluginTag = PluginTag("io", group = PluginTag.DATAFORGE_GROUP)

        override fun build(context: Context, meta: Meta): IOPlugin = IOPlugin(meta)

        public val WORK_DIRECTORY_KEY: Name = Name.of("io", "workDirectory")
    }
}

internal val ioContext = Context("IO") {
    plugin(IOPlugin)
}

public val Context.io: IOPlugin
    get() = if (this == Global) {
        ioContext.request(IOPlugin)
    } else {
        request(IOPlugin)
    }