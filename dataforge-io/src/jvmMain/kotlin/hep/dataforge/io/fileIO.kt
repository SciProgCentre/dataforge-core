package hep.dataforge.io

import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.isEmpty
import kotlinx.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.reflect.full.isSuperclassOf
import kotlin.streams.asSequence

public fun <R> Path.read(block: Input.() -> R): R = asBinary().read(block = block)

/**
 * Write a live output to a newly created file. If file does not exist, throws error
 */
public fun Path.write(block: Output.() -> Unit): Unit {
    val stream = Files.newOutputStream(this, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
    stream.asOutput().use(block)
}

/**
 * Create a new file or append to exiting one with given output [block]
 */
public fun Path.append(block: Output.() -> Unit): Unit {
    val stream = Files.newOutputStream(
        this,
        StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE
    )
    stream.asOutput().use(block)
}

/**
 * Create a new file or replace existing one using given output [block]
 */
public fun Path.rewrite(block: Output.() -> Unit): Unit {
    val stream = Files.newOutputStream(
        this,
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE
    )
    stream.asOutput().use(block)
}

public fun Path.readEnvelope(format: EnvelopeFormat): Envelope {
    val partialEnvelope: PartialEnvelope = asBinary().read {
        format.run { readPartial(this@read) }
    }
    val offset: Int = partialEnvelope.dataOffset.toInt()
    val size: Int = partialEnvelope.dataSize?.toInt() ?: (Files.size(this).toInt() - offset)
    val binary = FileBinary(this, offset, size)
    return SimpleEnvelope(partialEnvelope.meta, binary)
}

/**
 * Resolve IOFormat based on type
 */
@Suppress("UNCHECKED_CAST")
@DFExperimental
public inline fun <reified T : Any> IOPlugin.resolveIOFormat(): IOFormat<T>? {
    return ioFormatFactories.find { it.type.isSuperclassOf(T::class) } as IOFormat<T>?
}

/**
 * Read file containing meta using given [formatOverride] or file extension to infer meta type.
 * If [path] is a directory search for file starting with `meta` in it
 */
public fun IOPlugin.readMetaFile(
    path: Path,
    formatOverride: MetaFormat? = null,
    descriptor: NodeDescriptor? = null,
): Meta {
    if (!Files.exists(path)) error("Meta file $path does not exist")

    val actualPath: Path = if (Files.isDirectory(path)) {
        Files.list(path).asSequence().find { it.fileName.startsWith("meta") }
            ?: error("The directory $path does not contain meta file")
    } else {
        path
    }
    val extension = actualPath.fileName.toString().substringAfterLast('.')

    val metaFormat = formatOverride ?: resolveMetaFormat(extension) ?: error("Can't resolve meta format $extension")
    return metaFormat.run {
        actualPath.read {
            readMeta(this, descriptor)
        }
    }
}

/**
 * Write meta to file using [metaFormat]. If [path] is a directory, write a file with name equals name of [metaFormat].
 * Like "meta.json"
 */
public fun IOPlugin.writeMetaFile(
    path: Path,
    meta: Meta,
    metaFormat: MetaFormatFactory = JsonMetaFormat,
    descriptor: NodeDescriptor? = null,
) {
    val actualPath = if (Files.isDirectory(path)) {
        path.resolve("@" + metaFormat.name.toString())
    } else {
        path
    }
    metaFormat.run {
        actualPath.write {
            writeMeta(this, meta, descriptor)
        }
    }
}

/**
 * Return inferred [EnvelopeFormat] if only one format could read given file. If no format accepts file, return null. If
 * multiple formats accepts file, throw an error.
 */
public fun IOPlugin.peekBinaryFormat(path: Path): EnvelopeFormat? {
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

public val IOPlugin.Companion.META_FILE_NAME: String get() = "@meta"
public val IOPlugin.Companion.DATA_FILE_NAME: String get() = "@data"

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
public fun IOPlugin.readEnvelopeFile(
    path: Path,
    readNonEnvelopes: Boolean = false,
    formatPeeker: IOPlugin.(Path) -> EnvelopeFormat? = IOPlugin::peekBinaryFormat,
): Envelope? {
    if (!Files.exists(path)) return null

    //read two-files directory
    if (Files.isDirectory(path)) {
        val metaFile = Files.list(path).asSequence().singleOrNull {
            it.fileName.toString().startsWith(IOPlugin.META_FILE_NAME)
        }

        val meta = if (metaFile == null) {
            Meta.EMPTY
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
        path.readEnvelope(format)
    } ?: if (readNonEnvelopes) { // if no format accepts file, read it as binary
        SimpleEnvelope(Meta.EMPTY, path.asBinary())
    } else null
}

/**
 * Write a binary into file. Throws an error if file already exists
 */
public fun <T : Any> IOFormat<T>.writeToFile(path: Path, obj: T) {
    path.write {
        writeObject(this, obj)
    }
}

/**
 * Write envelope file to given [path] using [envelopeFormat] and optional [metaFormat]
 */
@DFExperimental
public fun IOPlugin.writeEnvelopeFile(
    path: Path,
    envelope: Envelope,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
    metaFormat: MetaFormatFactory? = null,
) {
    path.rewrite {
        envelopeFormat.writeEnvelope(this, envelope, metaFormat ?: envelopeFormat.defaultMetaFormat)
    }
}

/**
 * Write separate meta and data files to given directory [path]
 */
@DFExperimental
public fun IOPlugin.writeEnvelopeDirectory(
    path: Path,
    envelope: Envelope,
    metaFormat: MetaFormatFactory = JsonMetaFormat,
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
            val copied = copyTo(this@write)
            if (copied != envelope.data?.size) {
                error("The number of copied bytes does not equal data size")
            }
        }
    }
}