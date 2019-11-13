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
 * If [path] is a directory search for file starting with `meta` in it
 */
fun IOPlugin.readMetaFile(path: Path, formatOverride: MetaFormat? = null, descriptor: NodeDescriptor? = null): Meta {
    if (!Files.exists(path)) error("Meta file $path does not exist")

    val actualPath: Path = if (Files.isDirectory(path)) {
        Files.list(path).asSequence().find { it.fileName.startsWith("meta") }
            ?: error("The directory $path does not contain meta file")
    } else {
        path
    }
    val extension = actualPath.fileName.toString().substringAfterLast('.')

    val metaFormat = formatOverride ?: metaFormat(extension) ?: error("Can't resolve meta format $extension")
    return metaFormat.run {
        Files.newByteChannel(actualPath, StandardOpenOption.READ).asInput().use { it.readMeta(descriptor) }
    }
}

/**
 * Write meta to file using [metaFormat]. If [path] is a directory, write a file with name equals name of [metaFormat].
 * Like "meta.json"
 */
fun IOPlugin.writeMetaFile(
    path: Path,
    metaFormat: MetaFormatFactory = JsonMetaFormat,
    descriptor: NodeDescriptor? = null
) {
    val actualPath = if (Files.isDirectory(path)) {
        path.resolve(metaFormat.name.toString())
    } else {
        path
    }
    metaFormat.run {
        Files.newByteChannel(actualPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW).asOutput().use {
            it.writeMeta(meta, descriptor)
        }
    }
}

/**
 * Return inferred [EnvelopeFormat] if only one format could read given file. If no format accepts file, return null. If
 * multiple formats accepts file, throw an error.
 */
fun IOPlugin.peekBinaryFormat(binary: Binary): EnvelopeFormat? {
    val formats = envelopeFormatFactories.mapNotNull { factory ->
        binary.read {
            factory.peekFormat(this@peekBinaryFormat, this@read)
        }
    }

    return when (formats.size) {
        0 -> null
        1 -> formats.first()
        else -> error("Envelope format binary recognition clash")
    }
}

/**
 * Read and envelope from file if the file exists, return null if file does not exist.
 *
 * If file is directory, then expect two files inside:
 * * **meta.<format name>** for meta
 * * **data** for data
 *
 * If the file is envelope read it using [EnvelopeFormatFactory.peekFormat] functionality to infer format.
 *
 * If the file is not an envelope and [readNonEnvelopes] is true, return an Envelope without meta, using file as binary.
 *
 * Return null otherwise.
 */
@DFExperimental
fun IOPlugin.readEnvelopeFile(
    path: Path,
    readNonEnvelopes: Boolean = false,
    formatPeeker: IOPlugin.(Binary) -> EnvelopeFormat? = IOPlugin::peekBinaryFormat
): Envelope? {
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

    return formatPeeker(binary)?.run {
        binary.read {
            readObject()
        }
    } ?: if (readNonEnvelopes) { // if no format accepts file, read it as binary
        SimpleEnvelope(Meta.empty, binary)
    } else null
}

fun IOPlugin.writeEnvelopeFile(
    path: Path,
    envelope: Envelope,
    format: EnvelopeFormat = TaggedEnvelopeFormat
) {
    val output = Files.newByteChannel(
        path,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    ).asOutput()

    with(format) {
        output.writeObject(envelope)
    }
}