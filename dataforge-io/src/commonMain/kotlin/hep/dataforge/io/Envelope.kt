package hep.dataforge.io

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.plus
import kotlinx.io.Binary

public interface Envelope {
    public val meta: Meta
    public val data: Binary?

    public companion object {

        /**
         * meta keys
         */
        public val ENVELOPE_NODE_KEY: Name = "@envelope".asName()
        public val ENVELOPE_TYPE_KEY: Name = ENVELOPE_NODE_KEY + "type"
        public val ENVELOPE_DATA_TYPE_KEY: Name = ENVELOPE_NODE_KEY + "dataType"
        public val ENVELOPE_DATA_ID_KEY: Name = ENVELOPE_NODE_KEY + "dataID"
        public val ENVELOPE_DESCRIPTION_KEY: Name = ENVELOPE_NODE_KEY + "description"
        public val ENVELOPE_NAME_KEY: Name = ENVELOPE_NODE_KEY + "name"
        //const val ENVELOPE_TIME_KEY = "@envelope.time"

        /**
         * Build a static envelope using provided builder
         */
        public inline operator fun invoke(block: EnvelopeBuilder.() -> Unit): Envelope =
            EnvelopeBuilder().apply(block).seal()
    }
}

public class SimpleEnvelope(override val meta: Meta, override val data: Binary?) : Envelope

/**
 * The purpose of the envelope
 *
 */
public val Envelope.type: String? get() = meta[Envelope.ENVELOPE_TYPE_KEY].string

/**
 * The type of data encoding
 *
 */
public val Envelope.dataType: String? get() = meta[Envelope.ENVELOPE_DATA_TYPE_KEY].string

/**
 * Textual user friendly description
 *
 */
public val Envelope.description: String? get() = meta[Envelope.ENVELOPE_DESCRIPTION_KEY].string

/**
 * An optional unique identifier that is used for data comparison. Data without identifier could not be compared to another data.
 */
public val Envelope.dataID: String? get() = meta[Envelope.ENVELOPE_DATA_ID_KEY].string

public fun Envelope.metaEquals(other: Envelope): Boolean = this.meta == other.meta

public fun Envelope.dataEquals(other: Envelope): Boolean = this.dataID != null && this.dataID == other.dataID

public fun Envelope.contentEquals(other: Envelope): Boolean {
    return (this === other || (metaEquals(other) && dataEquals(other)))
}


/**
 * An envelope, which wraps existing envelope and adds one or several additional layers of meta
 */
public class ProxyEnvelope(public val source: Envelope, vararg meta: Meta) : Envelope {
    override val meta: Laminate = Laminate(*meta, source.meta)
    override val data: Binary? get() = source.data
}

/**
 * Add few meta layers to existing envelope (on top of existing meta)
 */
public fun Envelope.withMetaLayers(vararg layers: Meta): Envelope {
    return when {
        layers.isEmpty() -> this
        this is ProxyEnvelope -> ProxyEnvelope(source, *layers, *this.meta.layers.toTypedArray())
        else -> ProxyEnvelope(this, *layers)
    }
}
