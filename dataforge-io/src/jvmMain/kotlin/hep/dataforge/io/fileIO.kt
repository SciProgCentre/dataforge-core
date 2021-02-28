package hep.dataforge.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.isEmpty
import hep.dataforge.misc.DFExperimental
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.asOutput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.inputStream
import kotlin.math.min
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf
import kotlin.streams.asSequence


internal class PathBinary(
    private val path: Path,
    private val fileOffset: Int = 0,
    override val size: Int = Files.size(path).toInt() - fileOffset,
) : Binary {

    @OptIn(ExperimentalPathApi::class)
    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R {
        val actualOffset = offset + fileOffset
        val actualSize = min(atMost, size - offset)
        val array = path.inputStream().use {
            it.skip(actualOffset.toLong())
            it.readNBytes(actualSize)
        }
        return ByteReadPacket(array).block()
    }
}

public fun Path.asBinary(): Binary = PathBinary(this)

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
        format.run {
            readPartial(this@read)
        }
    }
    val offset: Int = partialEnvelope.dataOffset.toInt()
    val size: Int = partialEnvelope.dataSize?.toInt() ?: (Files.size(this).toInt() - offset)
    val binary = PathBinary(this, offset, size)
    return SimpleEnvelope(partialEnvelope.meta, binary)
}

/**
 * Resolve IOFormat based on type
 */
@Suppress("UNCHECKED_CAST")
@DFExperimental
public inline fun <reified T : Any> IOPlugin.resolveIOFormat(): IOFormat<T>? {
    return ioFormatFactories.find { it.type.isSupertypeOf(typeOf<T>()) } as IOFormat<T>?
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
public fun IOPlugin.peekFileEnvelopeFormat(path: Path): EnvelopeFormat? {
    val binary = path.asBinary()
    val formats = envelopeFormatFactories.mapNotNull { factory ->
        factory.peekFormat(this@peekFileEnvelopeFormat, binary)
    }

    return when (formats.size) {
        0 -> null
        1 -> formats.first()
        else -> error("Envelope format binary recognition clash: $formats")
    }
}

public val IOPlugin.Companion.META_FILE_NAME: String get() = "@meta"
public val IOPlugin.Companion.DATA_FILE_NAME: String get() = "@data"

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
@DFExperimental
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

    return formatPicker(path)?.let { format ->
        path.readEnvelope(format)
    } ?: if (readNonEnvelopes) { // if no format accepts file, read it as binary
        SimpleEnvelope(Meta.EMPTY, path.asBinary())
    } else error("Can't infer format for file $path")
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
            if (copied != envelope.data?.size?.toLong()) {
                error("The number of copied bytes does not equal data size")
            }
        }
    }
}