package hep.dataforge.io

import hep.dataforge.context.*
import hep.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import hep.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.toName
import kotlin.reflect.KClass

class IOPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    val ioFormatFactories by lazy {
        context.content<IOFormatFactory<*>>(IO_FORMAT_TYPE).values
    }

    fun <T : Any> resolveIOFormat(item: MetaItem<*>, type: KClass<out T>): IOFormat<T>? {
        val key = item.string ?: item.node[IO_FORMAT_NAME_KEY]?.string ?: error("Format name not defined")
        val name = key.toName()
        return ioFormatFactories.find { it.name == name }?.let {
            @Suppress("UNCHECKED_CAST")
            if (it.type != type) error("Format type ${it.type} is not the same as requested type $type")
            else it.invoke(item.node[IO_FORMAT_META_KEY].node ?: Meta.EMPTY, context) as IOFormat<T>
        }
    }


    val metaFormatFactories by lazy {
        context.content<MetaFormatFactory>(META_FORMAT_TYPE).values
    }

    fun resolveMetaFormat(key: Short, meta: Meta = Meta.EMPTY): MetaFormat? =
        metaFormatFactories.find { it.key == key }?.invoke(meta)

    fun resolveMetaFormat(name: String, meta: Meta = Meta.EMPTY): MetaFormat? =
        metaFormatFactories.find { it.shortName == name }?.invoke(meta)

    val envelopeFormatFactories by lazy {
        context.content<EnvelopeFormatFactory>(ENVELOPE_FORMAT_TYPE).values
    }

    fun resolveEnvelopeFormat(name: Name, meta: Meta = Meta.EMPTY): EnvelopeFormat? =
        envelopeFormatFactories.find { it.name == name }?.invoke(meta, context)

    fun resolveEnvelopeFormat(item: MetaItem<*>): EnvelopeFormat? {
        val name = item.string ?: item.node[IO_FORMAT_NAME_KEY]?.string ?: error("Envelope format name not defined")
        val meta = item.node[IO_FORMAT_META_KEY].node ?: Meta.EMPTY
        return resolveEnvelopeFormat(name.toName(), meta)
    }

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            META_FORMAT_TYPE -> defaultMetaFormats.toMap()
            ENVELOPE_FORMAT_TYPE -> defaultEnvelopeFormats.toMap()
            else -> super.provideTop(target)
        }
    }

    companion object : PluginFactory<IOPlugin> {
        val IO_FORMAT_NAME_KEY = "name".asName()
        val IO_FORMAT_META_KEY = "meta".asName()

        val defaultMetaFormats: List<MetaFormatFactory> = listOf(JsonMetaFormat, BinaryMetaFormat)
        val defaultEnvelopeFormats = listOf(TaggedEnvelopeFormat, TaglessEnvelopeFormat)

        override val tag: PluginTag = PluginTag("io", group = PluginTag.DATAFORGE_GROUP)

        override val type: KClass<out IOPlugin> = IOPlugin::class
        override fun invoke(meta: Meta, context: Context): IOPlugin = IOPlugin(meta)
    }
}

val Context.io: IOPlugin get() = plugins.fetch(IOPlugin)