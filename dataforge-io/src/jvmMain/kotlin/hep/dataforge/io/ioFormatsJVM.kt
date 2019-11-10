package hep.dataforge.io

import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.io.nio.asInput
import kotlinx.io.nio.asOutput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.reflect.full.isSuperclassOf
import kotlin.streams.asSequence

inline fun <reified T : Any> IOPlugin.resolveIOFormat(): IOFormat<T>? {
    return ioFormats.values.find { it.type.isSuperclassOf(T::class) } as IOFormat<T>?
}

/**
 * Read file containing meta using given [formatOverride] or file extension to infer meta type.
 */
fun IOPlugin.readMetaFile(path: Path, formatOverride: MetaFormat? = null, descriptor: NodeDescriptor? = null): Meta {
    if (!Files.exists(path)) error("Meta file $path does not exist")
    val extension = path.fileName.toString().substringAfterLast('.')

    val metaFormat = formatOverride ?: metaFormat(extension) ?: error("Can't resolve meta format $extension")
    return metaFormat.run {
        Files.newByteChannel(path, StandardOpenOption.READ).asInput().use { it.readMeta(descriptor) }
    }
}

fun IOPlugin.writeMetaFile(
    path: Path,
    metaFormat: MetaFormat = JsonMetaFormat,
    descriptor: NodeDescriptor? = null
) {
    metaFormat.run {
        Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW).asOutput().use {
            it.writeMeta(meta, descriptor)
        }
    }
}

/**
 * Read and envelope from file if the file exists, return null if file does not exist.
 */
@DFExperimental
fun IOPlugin.readEnvelopeFromFile(path: Path, readNonEnvelopes: Boolean = false): Envelope? {
    if (!Files.exists(path)) return null

    //read two-files directory
    if (Files.isDirectory(path)) {
        val metaFile = Files.list(path).asSequence()
            .singleOrNull { it.fileName.toString().startsWith("meta") }

        val meta = if (metaFile == null) {
            EmptyMeta
        } else {
            readMetaFile(metaFile)
        }

        val dataFile = path.resolve("data")

        val data: Binary? = if (Files.exists(dataFile)) {
            dataFile.asBinary()
        } else {
            null
        }

        return SimpleEnvelope(meta, data)
    }

    val binary = path.asBinary()

    val formats = envelopeFormatFactories.mapNotNull { factory ->
        binary.read {
            factory.peekFormat(this@readEnvelopeFromFile, this@read)
        }
    }
    return when (formats.size) {
        0 -> if (readNonEnvelopes) {
            SimpleEnvelope(Meta.empty, binary)
        } else {
            null
        }
        1 -> formats.first().run {
            binary.read {
                readObject()
            }
        }
        else -> error("Envelope format file recognition clash")
    }
}