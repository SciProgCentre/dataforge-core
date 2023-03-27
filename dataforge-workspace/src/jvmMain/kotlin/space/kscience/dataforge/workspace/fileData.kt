package space.kscience.dataforge.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.warn
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
import space.kscience.dataforge.workspace.FileData.Companion.DEFAULT_IGNORE_EXTENSIONS
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readAttributes
import kotlin.reflect.KType
import kotlin.reflect.typeOf


//public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

public typealias FileFormatResolver<T> = (path: Path, meta: Meta) -> IOReader<T>?

/**
 * A data based on a filesystem [Path]
 */
public class FileData<T> internal constructor(private val data: Data<T>, public val path: Path) : Data<T> by data {

    //    public val path: String? get() = meta[META_FILE_PATH_KEY].string
//    public val extension: String? get() = meta[META_FILE_EXTENSION_KEY].string
//
    public val createdTime: Instant? get() = meta[FILE_CREATE_TIME_KEY].string?.let { Instant.parse(it) }
    public val updatedTime: Instant? get() = meta[FILE_UPDATE_TIME_KEY].string?.let { Instant.parse(it) }

    public companion object {
        public val FILE_KEY: Name = "file".asName()
        public val FILE_PATH_KEY: Name = FILE_KEY + "path"
        public val FILE_EXTENSION_KEY: Name = FILE_KEY + "extension"
        public val FILE_CREATE_TIME_KEY: Name = FILE_KEY + "created"
        public val FILE_UPDATE_TIME_KEY: Name = FILE_KEY + "updated"
        public const val DF_FILE_EXTENSION: String = "df"
        public val DEFAULT_IGNORE_EXTENSIONS: Set<String> = setOf(DF_FILE_EXTENSION)
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
): FileData<T>? {
    val envelope = readEnvelopeFile(path, true)
    val format = formatResolver(path, envelope.meta) ?: return null
    val updatedMeta = envelope.meta.copy {
        FileData.FILE_PATH_KEY put path.toString()
        FileData.FILE_EXTENSION_KEY put path.extension

        val attributes = path.readAttributes<BasicFileAttributes>()
        FileData.FILE_UPDATE_TIME_KEY put attributes.lastModifiedTime().toInstant().toString()
        FileData.FILE_CREATE_TIME_KEY put attributes.creationTime().toInstant().toString()
    }
    return FileData(
        Data(format.type, updatedMeta) {
            (envelope.data ?: Binary.EMPTY).readWith(format)
        },
        path
    )
}


context(IOPlugin) @DFExperimental
private fun <T : Any> DataSetBuilder<T>.directory(
    path: Path,
    ignoreExtensions: Set<String>,
    formatResolver: FileFormatResolver<T>,
) {
    Files.list(path).forEach { childPath ->
        val fileName = childPath.fileName.toString()
        if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
            meta(readMetaFile(childPath))
        } else if (!fileName.startsWith("@")) {
            file(childPath, ignoreExtensions, formatResolver)
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
    ignoreExtensions: Set<String> = DEFAULT_IGNORE_EXTENSIONS,
    formatResolver: FileFormatResolver<T>,
): DataTree<T> {
    //read zipped data node
    if (path.fileName != null && path.fileName.toString().endsWith(".zip")) {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
            ?: error("Zip file system provider not found")
        val fs = fsProvider.newFileSystem(path, mapOf("create" to "true"))

        return readDataDirectory(type, fs.rootDirectories.first(), ignoreExtensions, formatResolver)
    }
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataTree(type) {
        meta {
            FileData.FILE_PATH_KEY put path.toString()
        }
        directory(path, ignoreExtensions, formatResolver)
    }
}

@OptIn(DFInternal::class)
@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataDirectory(
    path: Path,
    ignoreExtensions: Set<String> = DEFAULT_IGNORE_EXTENSIONS,
    noinline formatResolver: FileFormatResolver<T>,
): DataTree<T> = readDataDirectory(typeOf<T>(), path, ignoreExtensions, formatResolver)

/**
 * Read raw binary data tree from the directory. All files are read as-is (save for meta files).
 */
@DFExperimental
public fun IOPlugin.readRawDirectory(
    path: Path,
    ignoreExtensions: Set<String> = emptySet(),
): DataTree<Binary> = readDataDirectory(path, ignoreExtensions) { _, _ -> IOReader.binary }


private fun Path.toName() = Name(map { NameToken.parse(it.nameWithoutExtension) })

@DFInternal
@DFExperimental
public fun <T : Any> IOPlugin.monitorDataDirectory(
    type: KType,
    path: Path,
    ignoreExtensions: Set<String> = DEFAULT_IGNORE_EXTENSIONS,
    formatResolver: FileFormatResolver<T>,
): DataSource<T> {
    if (path.fileName.toString().endsWith(".zip")) error("Monitoring not supported for ZipFS")
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataSource(type, context) {
        directory(path, ignoreExtensions, formatResolver)
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
                                file(eventPath, ignoreExtensions, formatResolver)
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
    ignoreExtensions: Set<String> = DEFAULT_IGNORE_EXTENSIONS,
    noinline formatResolver: FileFormatResolver<T>,
): DataSource<T> = monitorDataDirectory(typeOf<T>(), path, ignoreExtensions, formatResolver)

/**
 * Read and monitor raw binary data tree from the directory. All files are read as-is (save for meta files).
 */
@DFExperimental
public fun IOPlugin.monitorRawDirectory(
    path: Path,
    ignoreExtensions: Set<String> = DEFAULT_IGNORE_EXTENSIONS,
): DataSource<Binary> = monitorDataDirectory(path, ignoreExtensions) { _, _ -> IOReader.binary }

/**
 * Write data tree to existing directory or create a new one using default [java.nio.file.FileSystem] provider
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeDataDirectory(
    path: Path,
    tree: DataTree<T>,
    format: IOWriter<T>,
    envelopeFormat: EnvelopeFormat? = null,
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
                        writeEnvelopeFile(childPath, envelope, envelopeFormat)
                    } else {
                        writeEnvelopeDirectory(childPath, envelope)
                    }
                }
            }
        }
        val treeMeta = tree.meta
        writeMetaFile(path, treeMeta)
    }
}

/**
 * Add file/directory-based data tree item
 *
 * @param ignoreExtensions a list of file extensions for which extension should be cut from the resulting item name
 */
context(IOPlugin)
@OptIn(DFInternal::class)
@DFExperimental
public fun <T : Any> DataSetBuilder<T>.file(
    path: Path,
    ignoreExtensions: Set<String> = DEFAULT_IGNORE_EXTENSIONS,
    formatResolver: FileFormatResolver<out T>,
) {

    fun defaultPath() = if (path.extension in ignoreExtensions) path.nameWithoutExtension else path.name

    try {
        //If path is a single file or a special directory, read it as single datum
        if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
            val data = readDataFile(path, formatResolver)
            if (data == null) {
                logger.warn { "File format is not resolved for $path. Skipping." }
                return
            }
            val name: String = data.meta[Envelope.ENVELOPE_NAME_KEY].string ?: defaultPath()
            data(name.asName(), data)
        } else {
            //otherwise, read as directory
            val data: DataTree<T> = readDataDirectory(dataType, path, ignoreExtensions, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string ?: defaultPath()
            node(name.asName(), data)
        }
    } catch (ex: Exception) {
        logger.error { "Failed to read file or directory at $path: ${ex.message}" }
    }
}

