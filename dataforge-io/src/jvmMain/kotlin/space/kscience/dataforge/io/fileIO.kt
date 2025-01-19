package space.kscience.dataforge.io


import kotlinx.coroutines.runBlocking
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.buffered
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.inputStream
import kotlin.math.min
import kotlin.streams.asSequence


internal class PathBinary(
    private val path: Path,
    private val fileOffset: Int = 0,
    override val size: Int = Files.size(path).toInt() - fileOffset,
) : Binary {

    override fun <R> read(offset: Int, atMost: Int, block: Source.() -> R): R = runBlocking {
        readSuspend(offset, atMost, block)
    }

    override suspend fun <R> readSuspend(offset: Int, atMost: Int, block: suspend Source.() -> R): R {
        val actualOffset = offset + fileOffset
        val actualSize = min(atMost, size - offset)
        val array = path.inputStream().use {
            it.skip(actualOffset.toLong())
            it.readNBytes(actualSize)
        }
        return ByteArraySource(array).buffered().use { it.block() }
    }

    override fun view(offset: Int, binarySize: Int) = PathBinary(path, fileOffset + offset, binarySize)
}

public fun Path.asBinary(): Binary = PathBinary(this)

public fun <R> Path.read(block: Source.() -> R): R = asBinary().read(block = block)

/**
 * Write a live output to a newly created file. If file does not exist, throws error
 */
public fun Path.write(block: Sink.() -> Unit): Unit {
    val stream = Files.newOutputStream(this, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
    stream.asSink().buffered().use(block)
}

/**
 * Create a new file or append to exiting one with given output [block]
 */
public fun Path.append(block: Sink.() -> Unit): Unit {
    val stream = Files.newOutputStream(
        this,
        StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE
    )
    stream.asSink().buffered().use(block)
}

/**
 * Create a new file or replace existing one using given output [block]
 */
public fun Path.rewrite(block: Sink.() -> Unit): Unit {
    val stream = Files.newOutputStream(
        this,
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE
    )
    stream.asSink().buffered().use(block)
}

public fun EnvelopeFormat.readFile(path: Path): Envelope = readFrom(path.asBinary())

public val IOPlugin.Companion.META_FILE_NAME: String get() = "@meta"
public val IOPlugin.Companion.DATA_FILE_NAME: String get() = "@data"

/**
 * Read file containing meta using given [formatOverride] or file extension to infer meta type.
 * If [path] is a directory search for file starting with `meta` in it.
 *
 * Returns null if meta could not be resolved
 */
public fun IOPlugin.readMetaFileOrNull(
    path: Path,
    formatOverride: MetaFormat? = null,
    descriptor: MetaDescriptor? = null,
): Meta? {
    if (!Files.exists(path)) return null

    val actualPath: Path = if (Files.isDirectory(path)) {
        Files.list(path).asSequence().find { it.fileName.startsWith(IOPlugin.META_FILE_NAME) }
            ?: return null
    } else {
        path
    }
    val extension = actualPath.fileName.toString().substringAfterLast('.')

    val metaFormat = formatOverride ?: resolveMetaFormat(extension) ?: return null
    return actualPath.read {
        metaFormat.readMeta(this, descriptor)
    }
}

/**
 * Read file containing meta using given [formatOverride] or file extension to infer meta type.
 * If [path] is a directory search for file starting with `meta` in it.
 *
 * Fails if nothing works.
 */
public fun IOPlugin.readMetaFile(
    path: Path,
    formatOverride: MetaFormat? = null,
    descriptor: MetaDescriptor? = null,
): Meta {
    if (!Files.exists(path)) error("Meta file $path does not exist")

    val actualPath: Path = if (Files.isDirectory(path)) {
        Files.list(path).asSequence().find { it.fileName.startsWith(IOPlugin.META_FILE_NAME) }
            ?: error("The directory $path does not contain meta file")
    } else {
        path
    }
    val extension = actualPath.fileName.toString().substringAfterLast('.')

    val metaFormat = formatOverride ?: resolveMetaFormat(extension) ?: error("Can't resolve meta format $extension")
    return actualPath.read {
        metaFormat.readMeta(this, descriptor)
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
    descriptor: MetaDescriptor? = null,
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
public fun IOPlugin.peekFileEnvelopeFormat(path: Path): EnvelopeFormat? {
    val binary = path.asBinary()
    return peekBinaryEnvelopeFormat(binary)
}


/**
 * Read and envelope from file if the file exists, return null if file does not exist.
 *
 * If file is directory, then expect two files inside:
 * * **meta.<meta format extension>** for meta
 * * **data** for data
 *
 * If the file is envelope read it using [EnvelopeFormatFactory.peekFormat] functionality to infer format (if not overridden with [formatPicker]).
 *
 * If the file is not an envelope and [readNonEnvelopes] is true, return an Envelope without meta, using file as binary.
 *
 * Return null otherwise.
 */
public fun IOPlugin.readEnvelopeFile(
    path: Path,
    readNonEnvelopes: Boolean = false,
    formatPicker: IOPlugin.(Path) -> EnvelopeFormat? = IOPlugin::peekFileEnvelopeFormat,
): Envelope {
    if (!Files.exists(path)) error("File with path $path does not exist")

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

    return formatPicker(path)?.readFile(path) ?: if (readNonEnvelopes) { // if no format accepts file, read it as binary
        SimpleEnvelope(Meta.EMPTY, path.asBinary())
    } else error("Can't infer format for file $path")
}

/**
 * Write envelope file to given [path] using [envelopeFormat] and optional [metaFormat]
 */
@DFExperimental
public fun IOPlugin.writeEnvelopeFile(
    path: Path,
    envelope: Envelope,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
) {
    path.rewrite {
        envelopeFormat.writeTo(this, envelope)
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
            val copied = transferTo(this@write)
            if (copied != envelope.data?.size?.toLong()) {
                error("The number of copied bytes does not equal data size")
            }
        }
    }
}