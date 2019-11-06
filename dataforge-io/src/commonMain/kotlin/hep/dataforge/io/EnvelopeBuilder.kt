package hep.dataforge.io

import hep.dataforge.meta.*
import kotlinx.io.core.Output
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes

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
    var dataID by metaBuilder.string(key = Envelope.ENVELOPE_DATA_ID_KEY)
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