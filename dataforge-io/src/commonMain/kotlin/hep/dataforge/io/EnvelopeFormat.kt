package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.provider.Type
import kotlinx.io.Input
import kotlinx.io.Output
import kotlin.reflect.KClass

/**
 * A partially read envelope with meta, but without data
 */
public data class PartialEnvelope(val meta: Meta, val dataOffset: UInt, val dataSize: ULong?)

public interface EnvelopeFormat : IOFormat<Envelope> {
    public val defaultMetaFormat: MetaFormatFactory get() = JsonMetaFormat

    public fun readPartial(input: Input): PartialEnvelope

    public fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory = defaultMetaFormat,
        formatMeta: Meta = Meta.EMPTY,
    )

    override fun readObject(input: Input): Envelope

    override fun writeObject(output: Output, obj: Envelope): Unit = writeEnvelope(output, obj)
}

public fun EnvelopeFormat.read(input: Input): Envelope = readObject(input)

@Type(ENVELOPE_FORMAT_TYPE)
public interface EnvelopeFormatFactory : IOFormatFactory<Envelope>, EnvelopeFormat {
    override val name: Name get() = "envelope".asName()
    override val type: KClass<out Envelope> get() = Envelope::class

    override fun invoke(meta: Meta, context: Context): EnvelopeFormat

    /**
     * Try to infer specific format from input and return null if the attempt is failed.
     * This method does **not** return Input into initial state.
     */
    public fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat?

    public companion object {
        public const val ENVELOPE_FORMAT_TYPE: String = "io.format.envelope"
    }
}