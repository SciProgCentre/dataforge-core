package hep.dataforge.io

import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.meta.isEmpty
import kotlinx.io.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.isSuperclassOf
import kotlin.streams.asSequence

/**
 * Resolve IOFormat based on type
 */
@DFExperimental
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
        actualPath.read{
            readMeta(descriptor)
        }
    }
}

/**
 * Write meta to file using [metaFormat]. If [path] is a directory, write a file with name equals name of [metaFormat].
 * Like "meta.json"
 */
fun IOPlugin.writeMetaFile(
    path: Path,
    meta: Meta,
    metaFormat: MetaFormatFactory = JsonMetaFormat,
    descriptor: NodeDescriptor? = null
) {
    val actualPath = if (Files.isDirectory(path)) {
        path.resolve("@" + metaFormat.name.toString())
    } else {
        path
    }
    metaFormat.run {
        actualPath.write{
            writeMeta(meta, descriptor)
        }
    }
}

/**
 * Return inferred [EnvelopeFormat] if only one format could read given file. If no format accepts file, return null. If
 * multiple formats accepts file, throw an error.
 */
fun IOPlugin.peekBinaryFormat(path: Path): EnvelopeFormat? {
    val binary = path.asBinary()
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

val IOPlugin.Companion.META_FILE_NAME: String get() = "@meta"
val IOPlugin.Companion.DATA_FILE_NAME: String get() = "@data"

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
    formatPeeker: IOPlugin.(Path) -> EnvelopeFormat? = IOPlugin::peekBinaryFormat
): Envelope? {
    if (!Files.exists(path)) return null

    //read two-files directory
    if (Files.isDirectory(path)) {
        val metaFile = Files.list(path).asSequence()
            .singleOrNull { it.fileName.toString().startsWith(IOPlugin.META_FILE_NAME) }

        val meta = if (metaFile == null) {
            EmptyMeta
        } else {
            readMetaFile(metaFile)
        }

        val dataFile = path.resolve(IOPlugin.DATA_FILE_NAME)

        val data: Binary? = if (Files.exists(dataFile)) {
            dataFile.asBinary()
        } else {
            null
        }

        return SimpleEnvelope(meta, data)
    }

    return formatPeeker(path)?.let { format ->
        FileEnvelope(path, format)
    } ?: if (readNonEnvelopes) { // if no format accepts file, read it as binary
        SimpleEnvelope(Meta.EMPTY, path.asBinary())
    } else null
}

/**
 * Write a binary into file. Throws an error if file already exists
 */
fun <T : Any> IOFormat<T>.writeToFile(path: Path, obj: T) {
    path.write {
        writeObject(obj)
    }
}

/**
 * Write envelope file to given [path] using [envelopeFormat] and optional [metaFormat]
 */
@DFExperimental
fun IOPlugin.writeEnvelopeFile(
    path: Path,
    envelope: Envelope,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
    metaFormat: MetaFormatFactory? = null
) {
    path.write {
        with(envelopeFormat) {
            writeEnvelope(envelope, metaFormat ?: envelopeFormat.defaultMetaFormat)
        }
    }
}

/**
 * Write separate meta and data files to given directory [path]
 */
@DFExperimental
fun IOPlugin.writeEnvelopeDirectory(
    path: Path,
    envelope: Envelope,
    metaFormat: MetaFormatFactory = JsonMetaFormat
) {
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
    if (!Files.isDirectory(path)) {
        error("Can't write envelope directory to file")
    }
    if (!envelope.meta.isEmpty()) {
        writeMetaFile(path, envelope.meta, metaFormat)
    }
    val dataFile = path.resolve(IOPlugin.DATA_FILE_NAME)
    dataFile.write {
        envelope.data?.read {
            val copied = writeInput(this)
            if (envelope.data?.size != Binary.INFINITE && copied != envelope.data?.size) {
                error("The number of copied bytes does not equal data size")
            }
        }
    }
}