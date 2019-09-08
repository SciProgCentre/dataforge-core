package hep.dataforge.io

import hep.dataforge.meta.*
import kotlinx.io.core.Output
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes

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

        /**
         * Build a static envelope using provided builder
         */
        fun build(block: EnvelopeBuilder.() -> Unit) = EnvelopeBuilder().apply(block).build()
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
 * An envelope, which wraps existing envelope and adds one or several additional layers of meta
 */
class ProxyEnvelope(val source: Envelope, vararg meta: Meta) : Envelope {
    override val meta: Laminate = Laminate(*meta, source.meta)
    override val data: Binary? get() = source.data
}

/**
 * Add few meta layers to existing envelope
 */
fun Envelope.withMetaLayers(vararg layers: Meta): Envelope {
    return when {
        layers.isEmpty() -> this
        this is ProxyEnvelope -> ProxyEnvelope(source, *layers, *this.meta.layers.toTypedArray())
        else -> ProxyEnvelope(this, *layers)
    }
}

class EnvelopeBuilder {
    private val metaBuilder = MetaBuilder()
    var data: Binary? = null

    fun meta(block: MetaBuilder.() -> Unit) {
        metaBuilder.apply(block)
    }

    fun meta(meta: Meta) {
        metaBuilder.update(meta)
    }

    var type by metaBuilder.string(key = Envelope.ENVELOPE_TYPE_KEY)
    var dataType by metaBuilder.string(key = Envelope.ENVELOPE_DATA_TYPE_KEY)
    var description by metaBuilder.string(key = Envelope.ENVELOPE_DESCRIPTION_KEY)

    /**
     * Construct a binary and transform it into byte-array based buffer
     */
    fun data(block: Output.() -> Unit) {
        val bytes = buildPacket {
            block()
        }
        data = ArrayBinary(bytes.readBytes())
    }

    internal fun build() = SimpleEnvelope(metaBuilder.seal(), data)
}