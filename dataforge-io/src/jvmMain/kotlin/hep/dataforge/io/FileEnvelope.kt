package hep.dataforge.io

import hep.dataforge.meta.Meta
import kotlinx.io.nio.asInput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileEnvelope internal constructor(val path: Path, val format: EnvelopeFormat) : Envelope {
    //TODO do not like this constructor. Hope to replace it later

    private val partialEnvelope: PartialEnvelope

    init {
        val input = Files.newByteChannel(path, StandardOpenOption.READ).asInput()
        partialEnvelope = format.readPartial(input)
    }

    override val meta: Meta get() = partialEnvelope.meta

    override val data: Binary? = FileBinary(path, partialEnvelope.dataOffset, partialEnvelope.dataSize)
}

fun Path.readEnvelope(format: EnvelopeFormat) = FileEnvelope(this,format)