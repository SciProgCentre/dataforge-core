package hep.dataforge.io

import hep.dataforge.context.*
import hep.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import hep.dataforge.io.MetaFormatFactory.Companion.META_FORMAT_TYPE
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.get
import kotlin.reflect.KClass

class IOPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    val metaFormatFactories by lazy {
        context.content<MetaFormatFactory>(META_FORMAT_TYPE).values
    }

    fun metaFormat(key: Short, meta: Meta = EmptyMeta): MetaFormat? =
        metaFormatFactories.find { it.key == key }?.invoke(meta)

    fun metaFormat(name: String, meta: Meta = EmptyMeta): MetaFormat? =
        metaFormatFactories.find { it.name.toString() == name }?.invoke(meta)

    val envelopeFormatFactories by lazy {
        context.content<EnvelopeFormatFactory>(ENVELOPE_FORMAT_TYPE).values
    }

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            META_FORMAT_TYPE -> defaultMetaFormats.toMap()
            ENVELOPE_FORMAT_TYPE -> defaultEnvelopeFormats.toMap()
            else -> super.provideTop(target)
        }
    }

    val ioFormats: Map<Name, IOFormatFactory<*>> by lazy {
        context.content<IOFormatFactory<*>>(IO_FORMAT_TYPE)
    }

    fun <T : Any> resolveIOFormat(item: MetaItem<*>, type: KClass<out T>): IOFormat<T>? {
        val key = item.string ?: item.node["name"]?.string ?: error("Format name not defined")
        return ioFormats[key]?.let {
            @Suppress("UNCHECKED_CAST")
            if (it.type != type) error("Format type ${it.type} is not the same as requested type $type")
            else it.invoke(item.node["meta"].node ?: EmptyMeta, context) as IOFormat<T>
        }
    }

    companion object : PluginFactory<IOPlugin> {
        val defaultMetaFormats: List<MetaFormatFactory> = listOf(JsonMetaFormat, BinaryMetaFormat)
        val defaultEnvelopeFormats = listOf(TaggedEnvelopeFormat)

        override val tag: PluginTag = PluginTag("io", group = PluginTag.DATAFORGE_GROUP)

        override val type: KClass<out IOPlugin> = IOPlugin::class
        override fun invoke(meta: Meta, context: Context): IOPlugin = IOPlugin(meta)
    }
}

val Context.io: IOPlugin get() = plugins.fetch(IOPlugin)