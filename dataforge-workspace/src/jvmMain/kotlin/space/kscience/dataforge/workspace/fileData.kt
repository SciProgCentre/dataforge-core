package space.kscience.dataforge.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.copy
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readAttributes
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.streams.toList


//public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

public typealias FileFormatResolver<T> = (path: Path, meta: Meta) -> IOReader<T>

public class FileData<T> internal constructor(private val data: Data<T>) : Data<T> by data {

    public val path: String? get() = meta[META_FILE_PATH_KEY].string
    public val extension: String? get() = meta[META_FILE_EXTENSION_KEY].string

    public val createdTime: Instant? get() = meta[META_FILE_CREATE_TIME_KEY].string?.let { Instant.parse(it) }
    public val updatedTime: Instant? get() = meta[META_FILE_UPDATE_TIME_KEY].string?.let { Instant.parse(it) }

    public companion object {
        public val META_FILE_KEY: Name = "file".asName()
        public val META_FILE_PATH_KEY: Name = META_FILE_KEY + "path"
        public val META_FILE_EXTENSION_KEY: Name = META_FILE_KEY + "extension"
        public val META_FILE_CREATE_TIME_KEY: Name = META_FILE_KEY + "created"
        public val META_FILE_UPDATE_TIME_KEY: Name = META_FILE_KEY + "updated"
    }
}


/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 */
@OptIn(DFInternal::class)
@DFExperimental
public fun <T : Any> IOPlugin.readDataFile(
    path: Path,
    formatResolver: FileFormatResolver<T>,
): FileData<T> {
    val envelope = readEnvelopeFile(path, true)
    val format = formatResolver(path, envelope.meta)
    val updatedMeta = envelope.meta.copy {
        FileData.META_FILE_PATH_KEY put path.toString()
        FileData.META_FILE_EXTENSION_KEY put path.extension

        val attributes = path.readAttributes<BasicFileAttributes>()
        FileData.META_FILE_UPDATE_TIME_KEY put attributes.lastModifiedTime().toInstant().toString()
        FileData.META_FILE_CREATE_TIME_KEY put attributes.creationTime().toInstant().toString()
    }
    return FileData(Data(format.type, updatedMeta) {
        envelope.data?.readWith(format) ?: error("Can't convert envelope without content to Data")
    })
}


context(IOPlugin) @DFExperimental
private fun <T : Any> DataSetBuilder<T>.directory(path: Path, formatResolver: FileFormatResolver<T>) {
    Files.list(path).toList().forEach { childPath ->
        val fileName = childPath.fileName.toString()
        if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
            meta(readMetaFile(childPath))
        } else if (!fileName.startsWith("@")) {
            file(childPath, formatResolver)
        }
    }
}

/**
 * Read the directory as a data node. If [path] is a zip archive, read it as directory
 */
@DFExperimental
@DFInternal
public fun <T : Any> IOPlugin.readDataDirectory(
    type: KType,
    path: Path,
    formatResolver: FileFormatResolver<T>,
): DataTree<T> {
    //read zipped data node
    if (path.fileName != null && path.fileName.toString().endsWith(".zip")) {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
            ?: error("Zip file system provider not found")
        val fs = fsProvider.newFileSystem(path, mapOf("create" to "true"))

        return readDataDirectory(type, fs.rootDirectories.first(), formatResolver)
    }
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataTree(type) {
        directory(path, formatResolver)
    }
}

@OptIn(DFInternal::class)
@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataDirectory(
    path: Path,
    noinline formatResolver: FileFormatResolver<T>,
): DataTree<Any> = readDataDirectory(typeOf<T>(), path, formatResolver)


@OptIn(DFExperimental::class)
private fun Path.toName() = Name(map { NameToken.parse(it.nameWithoutExtension) })

@DFInternal
@DFExperimental
public fun <T : Any> IOPlugin.monitorDataDirectory(
    type: KType,
    path: Path,
    formatResolver: FileFormatResolver<T>,
): DataSource<T> {
    if (path.fileName.toString().endsWith(".zip")) error("Monitoring not supported for ZipFS")
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataSource(type, context) {
        directory(path, formatResolver)
        launch(Dispatchers.IO) {
            val watchService = path.fileSystem.newWatchService()

            path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            )

            do {
                val key = watchService.take()
                if (key != null) {
                    for (event: WatchEvent<*> in key.pollEvents()) {
                        val eventPath = event.context() as Path
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            remove(eventPath.toName())
                        } else {
                            val fileName = eventPath.fileName.toString()
                            if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
                                meta(readMetaFile(eventPath))
                            } else if (!fileName.startsWith("@")) {
                                file(eventPath, formatResolver)
                            }
                        }
                    }
                    key.reset()
                }
            } while (isActive && key != null)
        }
    }
}


/**
 * Start monitoring given directory ([path]) as a [DataSource].
 */
@OptIn(DFInternal::class)
@DFExperimental
public inline fun <reified T : Any> IOPlugin.monitorDataDirectory(
    path: Path,
    noinline formatResolver: FileFormatResolver<T>,
): DataSource<T> = monitorDataDirectory(typeOf<T>(), path, formatResolver)

/**
 * Write data tree to existing directory or create a new one using default [java.nio.file.FileSystem] provider
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeDataDirectory(
    path: Path,
    tree: DataTree<T>,
    format: IOWriter<T>,
    envelopeFormat: EnvelopeFormat? = null,
    metaFormat: MetaFormatFactory? = null,
) {
    withContext(Dispatchers.IO) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        } else if (!Files.isDirectory(path)) {
            error("Can't write a node into file")
        }
        tree.items.forEach { (token, item) ->
            val childPath = path.resolve(token.toString())
            when (item) {
                is DataTreeItem.Node -> {
                    writeDataDirectory(childPath, item.tree, format, envelopeFormat)
                }
                is DataTreeItem.Leaf -> {
                    val envelope = item.data.toEnvelope(format)
                    if (envelopeFormat != null) {
                        writeEnvelopeFile(childPath, envelope, envelopeFormat, metaFormat)
                    } else {
                        writeEnvelopeDirectory(childPath, envelope, metaFormat ?: JsonMetaFormat)
                    }
                }
            }
        }
        val treeMeta = tree.meta
        writeMetaFile(path, treeMeta, metaFormat ?: JsonMetaFormat)
    }
}

/**
 * Add file/directory-based data tree item
 */
context(IOPlugin) @OptIn(DFInternal::class)
@DFExperimental
public fun <T : Any> DataSetBuilder<T>.file(
    path: Path,
    formatResolver: FileFormatResolver<out T>,
) {
    try {

        //If path is a single file or a special directory, read it as single datum
        if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
            val data = readDataFile(path, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string ?: path.nameWithoutExtension
            data(name, data)
        } else {
            //otherwise, read as directory
            val data = readDataDirectory(dataType, path, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string ?: path.nameWithoutExtension
            node(name, data)
        }
    } catch (ex: Exception) {
        logger.error { "Failed to read file or directory at $path: ${ex.message}" }
    }
}

