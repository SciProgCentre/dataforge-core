package space.kscience.dataforge.io

import space.kscience.dataforge.context.*
import space.kscience.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import space.kscience.dataforge.io.IOFormat.Companion.META_KEY
import space.kscience.dataforge.io.IOFormat.Companion.NAME_KEY
import space.kscience.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import space.kscience.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.toName
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

public class IOPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    public val ioFormatFactories: Collection<IOFormatFactory<*>> by lazy {
        context.gather<IOFormatFactory<*>>(IO_FORMAT_TYPE).values
    }

    public fun <T : Any> resolveIOFormat(item: Meta, type: KClass<out T>): IOFormat<T>? {
        val key = item.string ?: item[NAME_KEY]?.string ?: error("Format name not defined")
        val name = key.toName()
        return ioFormatFactories.find { it.name == name }?.let {
            @Suppress("UNCHECKED_CAST")
            if (it.type != type) error("Format type ${it.type} is not the same as requested type $type")
            else it.invoke(item[META_KEY] ?: Meta.EMPTY, context) as IOFormat<T>
        }
    }


    public val metaFormatFactories: Collection<MetaFormatFactory> by lazy {
        context.gather<MetaFormatFactory>(META_FORMAT_TYPE).values
    }

    public fun resolveMetaFormat(key: Short, meta: Meta = Meta.EMPTY): MetaFormat? =
        metaFormatFactories.find { it.key == key }?.invoke(meta)

    public fun resolveMetaFormat(name: String, meta: Meta = Meta.EMPTY): MetaFormat? =
        metaFormatFactories.find { it.shortName == name }?.invoke(meta)

    public val envelopeFormatFactories: Collection<EnvelopeFormatFactory> by lazy {
        context.gather<EnvelopeFormatFactory>(ENVELOPE_FORMAT_TYPE).values
    }

    private fun resolveEnvelopeFormat(name: Name, meta: Meta = Meta.EMPTY): EnvelopeFormat? =
        envelopeFormatFactories.find { it.name == name }?.invoke(meta, context)

    public fun resolveEnvelopeFormat(item: Meta): EnvelopeFormat? {
        val name = item.string ?: item[NAME_KEY]?.string ?: error("Envelope format name not defined")
        val meta = item[META_KEY] ?: Meta.EMPTY
        return resolveEnvelopeFormat(name.toName(), meta)
    }

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            META_FORMAT_TYPE -> defaultMetaFormats.toMap()
            ENVELOPE_FORMAT_TYPE -> defaultEnvelopeFormats.toMap()
            else -> super.content(target)
        }
    }

    public companion object : PluginFactory<IOPlugin> {
        public val defaultMetaFormats: List<MetaFormatFactory> = listOf(JsonMetaFormat)
        public val defaultEnvelopeFormats: List<EnvelopeFormatFactory> =
            listOf(TaggedEnvelopeFormat, TaglessEnvelopeFormat)

        override val tag: PluginTag = PluginTag("io", group = PluginTag.DATAFORGE_GROUP)

        override val type: KClass<out IOPlugin> = IOPlugin::class
        override fun invoke(meta: Meta, context: Context): IOPlugin = IOPlugin(meta)
    }
}

@ThreadLocal
internal val ioContext = Global.withEnv {
    name("IO")
    plugin(IOPlugin)
}

public val Context.io: IOPlugin get() = (if (this == Global) ioContext else this).fetch(IOPlugin)