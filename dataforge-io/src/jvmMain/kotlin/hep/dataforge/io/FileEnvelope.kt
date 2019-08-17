package hep.dataforge.io

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

fun Path.readEnvelope(format: EnvelopeFormat) = FileEnvelope(this, format)

fun Path.writeEnvelope(
    envelope: Envelope,
    format: EnvelopeFormat = TaggedEnvelopeFormat,
    metaFormat: MetaFormat = JsonMetaFormat
) {
    val output = Files.newByteChannel(
        this,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    ).asOutput()

    with(format) {
        output.writeEnvelope(envelope, metaFormat)
    }
}

