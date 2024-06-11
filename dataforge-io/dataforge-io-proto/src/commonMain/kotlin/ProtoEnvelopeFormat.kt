package pace.kscience.dataforge.io.proto

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import okio.ByteString
import okio.ByteString.Companion.toByteString
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.io.EnvelopeFormat
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.io.proto.ProtoEnvelope
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.Meta


public object ProtoEnvelopeFormat : EnvelopeFormat {
    override fun readFrom(source: Source): Envelope {
        val protoEnvelope = ProtoEnvelope.ADAPTER.decode(source.readByteArray())
        return Envelope(
            meta = protoEnvelope.meta?.let { ProtoMetaWrapper(it) } ?: Meta.EMPTY,
            data = protoEnvelope.dataBytes.toByteArray().asBinary()
        )
    }

    override fun writeTo(sink: Sink, obj: Envelope) {
        val protoEnvelope = ProtoEnvelope(
            obj.meta.toProto(),
            obj.data?.toByteArray()?.toByteString() ?: ByteString.EMPTY
        )
        sink.write(ProtoEnvelope.ADAPTER.encode(protoEnvelope))
    }
}