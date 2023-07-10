package space.kscience.dataforge.io

import kotlinx.io.Source
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.EnvelopeFormatFactory.Companion.ENVELOPE_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public interface EnvelopeFormat : IOFormat<Envelope> {

    override val type: KType get() = typeOf<Envelope>()
}

public fun EnvelopeFormat.read(input: Source): Envelope = readObject(input)

@Type(ENVELOPE_FORMAT_TYPE)
public interface EnvelopeFormatFactory : IOFormatFactory<Envelope>, EnvelopeFormat {
    override val type: KType get() = typeOf<Envelope>()

    override fun build(context: Context, meta: Meta): EnvelopeFormat

    /**
     * Try to infer specific format from input and return null if the attempt is failed.
     * This method does **not** return Input into initial state.
     */
    public fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat?

    public companion object {
        public val ENVELOPE_FACTORY_NAME: Name = "envelope".asName()
        public const val ENVELOPE_FORMAT_TYPE: String = "io.format.envelope"
    }
}