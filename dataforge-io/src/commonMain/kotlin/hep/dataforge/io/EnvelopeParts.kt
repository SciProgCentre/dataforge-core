package hep.dataforge.io

import hep.dataforge.context.Global
import hep.dataforge.io.EnvelopeParts.FORMAT_META_KEY
import hep.dataforge.io.EnvelopeParts.FORMAT_NAME_KEY
import hep.dataforge.io.EnvelopeParts.PARTS_DATA_TYPE
import hep.dataforge.io.EnvelopeParts.SIZE_KEY
import hep.dataforge.meta.*
import hep.dataforge.names.plus
import hep.dataforge.names.toName

object EnvelopeParts {
    val SIZE_KEY = Envelope.ENVELOPE_NODE_KEY + "parts" + "size"
    val FORMAT_NAME_KEY = Envelope.ENVELOPE_NODE_KEY + "parts" + "format"
    val FORMAT_META_KEY = Envelope.ENVELOPE_NODE_KEY + "parts" + "meta"

    const val PARTS_DATA_TYPE = "envelope.parts"
}

fun EnvelopeBuilder.parts(formatFactory: EnvelopeFormatFactory, envelopes: Collection<Envelope>) {
    dataType = PARTS_DATA_TYPE
    meta {
        SIZE_KEY put envelopes.size
        FORMAT_NAME_KEY put formatFactory.name.toString()
    }
    val format = formatFactory()
    data {
        format.run {
            envelopes.forEach {
                writeObject(it)
            }
        }
    }
}

fun EnvelopeBuilder.parts(formatFactory: EnvelopeFormatFactory, builder: suspend SequenceScope<Envelope>.() -> Unit) =
    parts(formatFactory, sequence(builder).toList())

fun Envelope.parts(io: IOPlugin = Global.plugins.fetch(IOPlugin)): Sequence<Envelope> {
    return when (dataType) {
        PARTS_DATA_TYPE -> {
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
        else -> emptySequence()
    }
}
