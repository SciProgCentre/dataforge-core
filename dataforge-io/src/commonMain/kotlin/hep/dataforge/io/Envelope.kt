package hep.dataforge.io

import hep.dataforge.context.Named
import hep.dataforge.io.EnvelopeFormat.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.io.IOPlugin.Companion.defaultMetaFormats
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.provider.Type
import kotlinx.io.core.Input
import kotlinx.io.core.Output

interface Envelope {
    val meta: Meta
    val data: Binary?

    companion object {

        /**
         * meta keys
         */
        const val ENVELOPE_NODE = "@envelope"
        const val ENVELOPE_TYPE_KEY = "$ENVELOPE_NODE.type"
        const val ENVELOPE_DATA_TYPE_KEY = "$ENVELOPE_NODE.dataType"
        const val ENVELOPE_DESCRIPTION_KEY = "$ENVELOPE_NODE.description"
        //const val ENVELOPE_TIME_KEY = "@envelope.time"

    }
}

class SimpleEnvelope(override val meta: Meta, override val data: Binary?) : Envelope

/**
 * The purpose of the envelope
 *
 * @return
 */
val Envelope.type: String? get() = meta[Envelope.ENVELOPE_TYPE_KEY].string

/**
 * The type of data encoding
 *
 * @return
 */
val Envelope.dataType: String? get() = meta[Envelope.ENVELOPE_DATA_TYPE_KEY].string

/**
 * Textual user friendly description
 *
 * @return
 */
val Envelope.description: String? get() = meta[Envelope.ENVELOPE_DESCRIPTION_KEY].string

/**
 * A partially read envelope with meta, but without data
 */
@ExperimentalUnsignedTypes
data class PartialEnvelope(val meta: Meta, val dataOffset: UInt, val dataSize: ULong?)

@Type(ENVELOPE_FORMAT_TYPE)
interface EnvelopeFormat : IOFormat<Envelope>, Named {
    fun Input.readPartial(formats: Collection<MetaFormat> = defaultMetaFormats): PartialEnvelope

    fun Input.readEnvelope(formats: Collection<MetaFormat> = defaultMetaFormats): Envelope

    override fun Input.readThis(): Envelope  = readEnvelope()

    fun Output.writeEnvelope(envelope: Envelope, format: MetaFormat = JsonMetaFormat)

    override fun Output.writeThis(obj: Envelope) = writeEnvelope(obj)

    companion object {
        const val ENVELOPE_FORMAT_TYPE = "envelopeFormat"
    }
}