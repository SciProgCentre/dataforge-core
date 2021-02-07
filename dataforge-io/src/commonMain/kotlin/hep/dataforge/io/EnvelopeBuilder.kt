package hep.dataforge.io

import hep.dataforge.meta.*
import kotlinx.io.*

public class EnvelopeBuilder : Envelope {
    private val metaBuilder = MetaBuilder()

    override var data: Binary? = null
    override var meta: Meta
        get() = metaBuilder
        set(value) {
            metaBuilder.update(value)
        }

    public fun meta(block: MetaBuilder.() -> Unit) {
        metaBuilder.apply(block)
    }

    /**
     * The general purpose of the envelope
     */
    public var type: String? by metaBuilder.string(key = Envelope.ENVELOPE_TYPE_KEY)
    public var dataType: String? by metaBuilder.string(key = Envelope.ENVELOPE_DATA_TYPE_KEY)

    /**
     * Data unique identifier to bypass identity checks
     */
    public var dataID: String? by metaBuilder.string(key = Envelope.ENVELOPE_DATA_ID_KEY)
    public var description: String? by metaBuilder.string(key = Envelope.ENVELOPE_DESCRIPTION_KEY)
    public var name: String? by metaBuilder.string(key = Envelope.ENVELOPE_NAME_KEY)

    /**
     * Construct a data binary from given builder
     */
    @OptIn(ExperimentalIoApi::class)
    public fun data(block: Output.() -> Unit) {
        val arrayBuilder = ByteArrayOutput()
        arrayBuilder.block()
        data = arrayBuilder.toByteArray().asBinary()
    }

    public fun seal(): Envelope = SimpleEnvelope(metaBuilder.seal(), data)

}


/**
 * Build a static envelope using provided [builder]
 */
public inline fun Envelope(builder: EnvelopeBuilder.() -> Unit): Envelope = EnvelopeBuilder().apply(builder).seal()

//@ExperimentalContracts
//suspend fun EnvelopeBuilder.buildData(block: suspend Output.() -> Unit): Binary{
//    contract {
//        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//    }
//    val scope = CoroutineScope(coroutineContext)
//}