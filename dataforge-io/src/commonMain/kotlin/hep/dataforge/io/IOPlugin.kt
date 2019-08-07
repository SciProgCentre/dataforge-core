package hep.dataforge.io

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.context.PluginFactory
import hep.dataforge.context.PluginTag
import hep.dataforge.context.content
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import kotlin.reflect.KClass

class IOPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    val metaFormats by lazy {
        context.content<MetaFormat>(MetaFormat.META_FORMAT_TYPE).values
    }

    fun metaFormat(key: Short): MetaFormat? = metaFormats.find { it.key == key }
    fun metaFormat(name: String): MetaFormat? = metaFormats.find { it.name == name }

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            MetaFormat.META_FORMAT_TYPE -> internalMetaFormats
            EnvelopeFormat.ENVELOPE_FORMAT_TYPE -> mapOf(
                TaggedEnvelopeFormat.VERSION.asName() to TaggedEnvelopeFormat(metaFormats)
            )
            else -> super.provideTop(target)
        }
    }

    companion object : PluginFactory<IOPlugin> {
        private val internalMetaFormats = listOf(JsonMetaFormat, BinaryMetaFormat).toMap()

        override val tag: PluginTag = PluginTag("io", group = PluginTag.DATAFORGE_GROUP)
        override val type: KClass<out IOPlugin> = IOPlugin::class
        override fun invoke(meta: Meta): IOPlugin = IOPlugin(meta)
    }
}