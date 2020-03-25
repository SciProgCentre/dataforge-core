package hep.dataforge.io

import hep.dataforge.meta.Meta
import kotlinx.io.Binary
import kotlinx.io.ExperimentalIoApi
import kotlinx.io.FileBinary
import kotlinx.io.read
import java.nio.file.Path

@ExperimentalIoApi
class FileEnvelope internal constructor(val path: Path, val format: EnvelopeFormat) : Envelope {
    //TODO do not like this constructor. Hope to replace it later

    private val partialEnvelope: PartialEnvelope = path.read {
        format.run { readPartial() }
    }

    override val meta: Meta get() = partialEnvelope.meta

    override val data: Binary? = FileBinary(path, partialEnvelope.dataOffset.toInt(), partialEnvelope.dataSize?.toInt())
}

