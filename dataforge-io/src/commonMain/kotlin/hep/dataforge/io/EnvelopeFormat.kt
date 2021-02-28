package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.misc.Type
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A partially read envelope with meta, but without data
 */
public data class PartialEnvelope(val meta: Meta, val dataOffset: UInt, val dataSize: ULong?)

public interface EnvelopeFormat : IOFormat<Envelope> {
    override val type: KType get() = typeOf<Envelope>()

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
    override val type: KType get() = typeOf<Envelope>()

    override fun invoke(meta: Meta, context: Context): EnvelopeFormat

    /**
     * Try to infer specific format from input and return null if the attempt is failed.
     * This method does **not** return Input into initial state.
     */
    public fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat?

    public companion object {
        public const val ENVELOPE_FORMAT_TYPE: String = "io.format.envelope"
    }
}