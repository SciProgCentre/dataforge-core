package hep.dataforge.io

import hep.dataforge.meta.*
import kotlinx.io.*

class EnvelopeBuilder {
    private val metaBuilder = MetaBuilder()
    var data: Binary? = null

    fun meta(block: MetaBuilder.() -> Unit) {
        metaBuilder.apply(block)
    }

    fun meta(meta: Meta) {
        metaBuilder.update(meta)
    }

    /**
     * The general purpose of the envelope
     */
    var type by metaBuilder.string(key = Envelope.ENVELOPE_TYPE_KEY)
    var dataType by metaBuilder.string(key = Envelope.ENVELOPE_DATA_TYPE_KEY)

    /**
     * Data unique identifier to bypass identity checks
     */
    var dataID by metaBuilder.string(key = Envelope.ENVELOPE_DATA_ID_KEY)
    var description by metaBuilder.string(key = Envelope.ENVELOPE_DESCRIPTION_KEY)
    var name by metaBuilder.string(key = Envelope.ENVELOPE_NAME_KEY)

    /**
     * Construct a data binary from given builder
     */
    @OptIn(ExperimentalIoApi::class)
    fun data(block: Output.() -> Unit) {
        val arrayBuilder = ByteArrayOutput()
        arrayBuilder.block()
        data = arrayBuilder.toByteArray().asBinary()
    }

    fun build() = SimpleEnvelope(metaBuilder.seal(), data)

}

//@ExperimentalContracts
//suspend fun EnvelopeBuilder.buildData(block: suspend Output.() -> Unit): Binary{
//    contract {
//        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//    }
//    val scope = CoroutineScope(coroutineContext)
//}