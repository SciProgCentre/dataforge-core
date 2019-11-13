package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.provider.Type
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlin.reflect.KClass

/**
 * A partially read envelope with meta, but without data
 */
@ExperimentalUnsignedTypes
data class PartialEnvelope(val meta: Meta, val dataOffset: UInt, val dataSize: ULong?)


interface EnvelopeFormat : IOFormat<Envelope> {
    val defaultMetaFormat: MetaFormatFactory get() = JsonMetaFormat

    fun Input.readPartial(): PartialEnvelope

    fun Output.writeEnvelope(envelope: Envelope, metaFormatFactory: MetaFormatFactory, formatMeta: Meta = EmptyMeta)

    override fun Input.readObject(): Envelope

    override fun Output.writeObject(obj: Envelope): Unit = writeEnvelope(obj, defaultMetaFormat)
}

@Type(ENVELOPE_FORMAT_TYPE)
interface EnvelopeFormatFactory : IOFormatFactory<Envelope>, EnvelopeFormat {
    override val name: Name get() = "envelope".asName()
    override val type: KClass<out Envelope> get() = Envelope::class

    override fun invoke(meta: Meta, context: Context): EnvelopeFormat

    /**
     * Try to infer specific format from input and return null if the attempt is failed.
     * This method does **not** return Input into initial state.
     */
    fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat?

    companion object {
        const val ENVELOPE_FORMAT_TYPE = "io.format.envelope"
    }
}