package hep.dataforge.io

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.io.nio.asInput
import kotlinx.io.nio.asOutput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileEnvelope internal constructor(val path: Path, val format: EnvelopeFormat) : Envelope {
    //TODO do not like this constructor. Hope to replace it later

    private val partialEnvelope: PartialEnvelope

    init {
        val input = Files.newByteChannel(path, StandardOpenOption.READ).asInput()
        partialEnvelope = format.run { input.readPartial() }
    }

    override val meta: Meta get() = partialEnvelope.meta

    override val data: Binary? = FileBinary(path, partialEnvelope.dataOffset, partialEnvelope.dataSize)
}

fun IOPlugin.readEnvelopeFile(
    path: Path,
    formatFactory: EnvelopeFormatFactory = TaggedEnvelopeFormat,
    formatMeta: Meta = EmptyMeta
): FileEnvelope {
    val format = formatFactory(formatMeta, context)
    return FileEnvelope(path, format)
}

fun IOPlugin.writeEnvelopeFile(
    path: Path,
    envelope: Envelope,
    formatFactory: EnvelopeFormatFactory = TaggedEnvelopeFormat,
    formatMeta: Meta = EmptyMeta
) {
    val output = Files.newByteChannel(
        path,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    ).asOutput()

    with(formatFactory(formatMeta, context)) {
        output.writeThis(envelope)
    }
}

