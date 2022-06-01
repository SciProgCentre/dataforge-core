package space.kscience.dataforge.io

import space.kscience.dataforge.context.invoke
import space.kscience.dataforge.io.Envelope.Companion.ENVELOPE_NODE_KEY
import space.kscience.dataforge.io.PartDescriptor.Companion.DEFAULT_MULTIPART_DATA_SEPARATOR
import space.kscience.dataforge.io.PartDescriptor.Companion.MULTIPART_DATA_TYPE
import space.kscience.dataforge.io.PartDescriptor.Companion.MULTIPART_KEY
import space.kscience.dataforge.io.PartDescriptor.Companion.PARTS_KEY
import space.kscience.dataforge.io.PartDescriptor.Companion.PART_FORMAT_KEY
import space.kscience.dataforge.io.PartDescriptor.Companion.SEPARATOR_KEY
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus

private class PartDescriptor : Scheme() {
    var offset by int(0)
    var size by int(0)
    var partMeta by node("meta".asName())

    companion object : SchemeSpec<PartDescriptor>(::PartDescriptor) {
        val MULTIPART_KEY = ENVELOPE_NODE_KEY + "multipart"
        val PARTS_KEY = MULTIPART_KEY + "parts"
        val SEPARATOR_KEY = MULTIPART_KEY + "separator"

        const val DEFAULT_MULTIPART_DATA_SEPARATOR = "\r\n#~PART~#\r\n"

        val PART_FORMAT_KEY = "format".asName()

        const val MULTIPART_DATA_TYPE = "envelope.multipart"
    }
}

public data class EnvelopePart(val binary: Binary, val description: Meta?)

public typealias EnvelopeParts = List<EnvelopePart>

public fun EnvelopeBuilder.multipart(
    parts: EnvelopeParts,
    separator: String = DEFAULT_MULTIPART_DATA_SEPARATOR,
) {
    dataType = MULTIPART_DATA_TYPE

    var offsetCounter = 0
    val separatorSize = separator.length
    val partDescriptors = parts.map { (binary, description) ->
        offsetCounter += separatorSize
        PartDescriptor {
            offset = offsetCounter
            size = binary.size
            partMeta = description
        }.also {
            offsetCounter += binary.size
        }
    }

    meta {
        if (separator != DEFAULT_MULTIPART_DATA_SEPARATOR) {
            SEPARATOR_KEY put separator
        }
        setIndexed(PARTS_KEY, partDescriptors.map { it.toMeta() })
    }

    data {
        parts.forEach {
            writeRawString(separator)
            writeBinary(it.binary)
        }
    }
}

/**
 * Put a list of envelopes as parts of given envelope
 */
public fun EnvelopeBuilder.envelopes(
    envelopes: List<Envelope>,
    formatFactory: EnvelopeFormatFactory = TaggedEnvelopeFormat,
    formatMeta: Meta? = null,
    separator: String = DEFAULT_MULTIPART_DATA_SEPARATOR,
) {
    val parts = envelopes.map {
        val format = formatMeta?.let { formatFactory(formatMeta) } ?: formatFactory
        val binary = Binary(it, format)
        EnvelopePart(binary, null)
    }
    meta {
        (MULTIPART_KEY + PART_FORMAT_KEY) put {
            IOFormatFactory.NAME_KEY put formatFactory.name.toString()
            formatMeta?.let { IOFormatFactory.META_KEY put formatMeta }
        }
    }
    multipart(parts, separator)
}

public fun Envelope.parts(): EnvelopeParts {
    if (data == null) return emptyList()
    //TODO add zip folder reader
    val parts = meta.getIndexed(PARTS_KEY).values.map {
        PartDescriptor.read(it)
    }
    return if (parts.isEmpty()) {
        listOf(EnvelopePart(data!!, meta[MULTIPART_KEY]))
    } else {
        parts.map {
            val binary = data!!.view(it.offset, it.size)
            val meta = Laminate(it.partMeta, meta[MULTIPART_KEY])
            EnvelopePart(binary, meta)
        }
    }
}

public fun EnvelopePart.envelope(format: EnvelopeFormat): Envelope = binary.readWith(format)

public val EnvelopePart.name: String? get() = description?.get("name").string

/**
 * Represent envelope part by an envelope
 */
public fun EnvelopePart.envelope(plugin: IOPlugin): Envelope {
    val formatItem = description?.get(PART_FORMAT_KEY)
    return if (formatItem != null) {
        val format: EnvelopeFormat = plugin.resolveEnvelopeFormat(formatItem)
            ?: error("Envelope format for $formatItem is not resolved")
        binary.readWith(format)
    } else {
        error("Envelope description not found")
        //SimpleEnvelope(description ?: Meta.EMPTY, binary)
    }
}