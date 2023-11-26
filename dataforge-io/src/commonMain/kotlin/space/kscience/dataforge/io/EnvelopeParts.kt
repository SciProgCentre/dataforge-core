package space.kscience.dataforge.io

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.write
import space.kscience.dataforge.io.Envelope.Companion.ENVELOPE_NODE_KEY
import space.kscience.dataforge.io.PartDescriptor.Companion.DEFAULT_MULTIPART_DATA_SEPARATOR
import space.kscience.dataforge.io.PartDescriptor.Companion.MULTIPART_DATA_TYPE
import space.kscience.dataforge.io.PartDescriptor.Companion.MULTIPART_KEY
import space.kscience.dataforge.io.PartDescriptor.Companion.PARTS_KEY
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

        val DEFAULT_MULTIPART_DATA_SEPARATOR = "\r\n#~PART~#\r\n".toAsciiByteString()

        const val MULTIPART_DATA_TYPE = "envelope.multipart"
    }
}

public data class EnvelopePart(val binary: Binary, val description: Meta?)

public typealias EnvelopeParts = List<EnvelopePart>

public fun EnvelopeBuilder.multipart(
    parts: EnvelopeParts,
    separator: ByteString = DEFAULT_MULTIPART_DATA_SEPARATOR,
) {
    dataType = MULTIPART_DATA_TYPE

    var offsetCounter = 0
    val separatorSize = separator.size
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
            SEPARATOR_KEY put separator.decodeToString()
        }
        setIndexed(PARTS_KEY, partDescriptors.map { it.toMeta() })
    }

    data {
        parts.forEach {
            write(separator)
            writeBinary(it.binary)
        }
    }
}

/**
 * Put a list of envelopes as parts of given envelope
 */
public fun EnvelopeBuilder.envelopes(
    envelopes: List<Envelope>,
    separator: ByteString = DEFAULT_MULTIPART_DATA_SEPARATOR,
) {
    val parts = envelopes.map {
        val binary = Binary(it, TaggedEnvelopeFormat)
        EnvelopePart(binary, null)
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
public fun EnvelopePart.envelope(): Envelope = binary.readWith(TaggedEnvelopeFormat)