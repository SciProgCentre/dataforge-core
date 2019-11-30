package hep.dataforge.io

import hep.dataforge.context.Global
import hep.dataforge.io.EnvelopeParts.FORMAT_META_KEY
import hep.dataforge.io.EnvelopeParts.FORMAT_NAME_KEY
import hep.dataforge.io.EnvelopeParts.INDEX_KEY
import hep.dataforge.io.EnvelopeParts.MULTIPART_DATA_TYPE
import hep.dataforge.io.EnvelopeParts.SIZE_KEY
import hep.dataforge.meta.*
import hep.dataforge.names.asName
import hep.dataforge.names.plus
import hep.dataforge.names.toName

object EnvelopeParts {
    val MULTIPART_KEY = "multipart".asName()
    val SIZE_KEY = Envelope.ENVELOPE_NODE_KEY + MULTIPART_KEY + "size"
    val INDEX_KEY = Envelope.ENVELOPE_NODE_KEY + MULTIPART_KEY + "index"
    val FORMAT_NAME_KEY = Envelope.ENVELOPE_NODE_KEY + MULTIPART_KEY + "format"
    val FORMAT_META_KEY = Envelope.ENVELOPE_NODE_KEY + MULTIPART_KEY + "meta"

    const val MULTIPART_DATA_TYPE = "envelope.multipart"
}

/**
 * Append multiple serialized envelopes to the data block. Previous data is erased if it was present
 */
@DFExperimental
fun EnvelopeBuilder.multipart(
    envelopes: Collection<Envelope>,
    format: EnvelopeFormatFactory,
    formatMeta: Meta = EmptyMeta
) {
    dataType = MULTIPART_DATA_TYPE
    meta {
        SIZE_KEY put envelopes.size
        FORMAT_NAME_KEY put format.name.toString()
        if (!formatMeta.isEmpty()) {
            FORMAT_META_KEY put formatMeta
        }
    }
    data {
        format(formatMeta).run {
            envelopes.forEach {
                writeEnvelope(it)
            }
        }
    }
}

/**
 * Create a multipart partition in the envelope adding additional name-index mapping in meta
 */
@DFExperimental
fun EnvelopeBuilder.multipart(
    envelopes: Map<String, Envelope>,
    format: EnvelopeFormatFactory,
    formatMeta: Meta = EmptyMeta
) {
    dataType = MULTIPART_DATA_TYPE
    meta {
        SIZE_KEY put envelopes.size
        FORMAT_NAME_KEY put format.name.toString()
        if (!formatMeta.isEmpty()) {
            FORMAT_META_KEY put formatMeta
        }
    }
    data {
        format.run {
            var counter = 0
            envelopes.forEach { (key, envelope) ->
                writeObject(envelope)
                meta {
                    append(INDEX_KEY, buildMeta {
                        "key" put key
                        "index" put counter
                    })
                }
                counter++
            }
        }
    }
}

@DFExperimental
fun EnvelopeBuilder.multipart(
    formatFactory: EnvelopeFormatFactory,
    formatMeta: Meta = EmptyMeta,
    builder: suspend SequenceScope<Envelope>.() -> Unit
) = multipart(sequence(builder).toList(), formatFactory, formatMeta)

/**
 * If given envelope supports multipart data, return a sequence of those parts (could be empty). Otherwise return null.
 */
@DFExperimental
fun Envelope.parts(io: IOPlugin = Global.plugins.fetch(IOPlugin)): Sequence<Envelope>? {
    return when (dataType) {
        MULTIPART_DATA_TYPE -> {
            val size = meta[SIZE_KEY].int ?: error("Unsized parts not supported yet")
            val formatName = meta[FORMAT_NAME_KEY].string?.toName()
                ?: error("Inferring parts format is not supported at the moment")
            val formatMeta = meta[FORMAT_META_KEY].node ?: EmptyMeta
            val format = io.envelopeFormat(formatName, formatMeta)
                ?: error("Format $formatName is not resolved by $io")
            return format.run {
                data?.read {
                    sequence {
                        repeat(size) {
                            yield(readObject())
                        }
                    }
                } ?: emptySequence()
            }
        }
        else -> null
    }
}
