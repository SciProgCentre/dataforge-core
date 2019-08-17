package hep.dataforge.io

import hep.dataforge.context.Named
import hep.dataforge.io.EnvelopeFormat.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.provider.Type
import kotlinx.io.core.Input
import kotlinx.io.core.Output

/**
 * A partially read envelope with meta, but without data
 */
@ExperimentalUnsignedTypes
data class PartialEnvelope(val meta: Meta, val dataOffset: UInt, val dataSize: ULong?)

@Type(ENVELOPE_FORMAT_TYPE)
interface EnvelopeFormat : IOFormat<Envelope>, Named {
    fun Input.readPartial(formats: Collection<MetaFormat> = IOPlugin.defaultMetaFormats): PartialEnvelope

    fun Input.readEnvelope(formats: Collection<MetaFormat> = IOPlugin.defaultMetaFormats): Envelope

    override fun Input.readThis(): Envelope = readEnvelope()

    fun Output.writeEnvelope(envelope: Envelope, format: MetaFormat = JsonMetaFormat)

    override fun Output.writeThis(obj: Envelope) = writeEnvelope(obj)

    companion object {
        const val ENVELOPE_FORMAT_TYPE = "envelopeFormat"
    }
}