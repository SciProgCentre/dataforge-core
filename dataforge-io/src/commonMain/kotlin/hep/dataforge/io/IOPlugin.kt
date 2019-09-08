package hep.dataforge.io

import hep.dataforge.context.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.get
import kotlin.reflect.KClass

class IOPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    val metaFormats by lazy {
        context.content<MetaFormat>(MetaFormat.META_FORMAT_TYPE).values
    }

    fun metaFormat(key: Short): MetaFormat? = metaFormats.find { it.key == key }
    fun metaFormat(name: String): MetaFormat? = metaFormats.find { it.name.toString() == name }

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            MetaFormat.META_FORMAT_TYPE -> defaultMetaFormats.toMap()
            EnvelopeFormat.ENVELOPE_FORMAT_TYPE -> defaultEnvelopeFormats.toMap()
            IOFormat.TYPE -> defaultIOFormats.toMap()
            else -> super.provideTop(target)
        }
    }

    val ioFormats: Map<Name, IOFormat<*>> by lazy {
        context.content<IOFormat<*>>(IOFormat.TYPE)
    }

    fun resolveIOFormat(item: MetaItem<*>): IOFormat<*>? {
        val key = item.string ?: error("Not a string value!")
        return ioFormats[key]
    }

    companion object : PluginFactory<IOPlugin> {
        val defaultMetaFormats: List<MetaFormat> = listOf(JsonMetaFormat, BinaryMetaFormat)
        val defaultEnvelopeFormats = listOf(TaggedEnvelopeFormat)

        val defaultIOFormats = listOf(
            DoubleIOFormat,
            ValueIOFormat,
            BinaryMetaFormat
        )

        override val tag: PluginTag = PluginTag("io", group = PluginTag.DATAFORGE_GROUP)
        override val type: KClass<out IOPlugin> = IOPlugin::class
        override fun invoke(meta: Meta): IOPlugin = IOPlugin(meta)
    }
}