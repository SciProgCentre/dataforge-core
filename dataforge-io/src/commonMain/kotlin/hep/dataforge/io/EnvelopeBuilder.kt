package hep.dataforge.io

import hep.dataforge.meta.*
import kotlinx.io.ArrayBinary
import kotlinx.io.Binary
import kotlinx.io.ExperimentalIoApi
import kotlinx.io.Output

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
    var name by metaBuilder.string(key = Envelope.ENVELOPE_NAME_KEY)

    /**
     * Construct a data binary from given builder
     */
    @ExperimentalIoApi
    fun data(block: Output.() -> Unit) {
        data = ArrayBinary.write(builder = block)
    }

    internal fun build() = SimpleEnvelope(metaBuilder.seal(), data)
}