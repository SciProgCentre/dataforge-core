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
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.*
import space.kscience.dataforge.workspace.FileData.Companion.defaultPathToName
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.time.Instant
import kotlin.io.path.*
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

        /**
         * Transform file name into DataForg name. Ignores DataForge file extensions.
         */
        public val defaultPathToName: (Path) -> Name = { path ->
            Name(
                path.map { segment ->
                    if (segment.isRegularFile() && segment.extension in DEFAULT_IGNORE_EXTENSIONS) {
                        NameToken(path.nameWithoutExtension)
                    } else {
                        NameToken(path.name)
                    }
                }
            )
        }
    }
}


/**
 * Read data with supported envelope format and binary format. If the envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read the meta header. The reading of envelope body is lazy
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
public fun <T : Any> DataSetBuilder<T>.directory(
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
    formatResolver: FileFormatResolver<T>,
) {
    Files.list(path).forEach { childPath ->
        val fileName = childPath.fileName.toString()
        if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
            meta(readMetaFile(childPath))
        } else if (!fileName.startsWith("@")) {
            file(childPath, pathToName, formatResolver)
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
    pathToName: (Path) -> Name = defaultPathToName,
    formatResolver: FileFormatResolver<T>,
): LegacyDataTree<T> {
    //read zipped data node
    if (path.fileName != null && path.fileName.toString().endsWith(".zip")) {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
            ?: error("Zip file system provider not found")
        val fs = fsProvider.newFileSystem(path, mapOf("create" to "true"))

        return readDataDirectory(type, fs.rootDirectories.first(), pathToName, formatResolver)
    }
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataTree(type) {
        meta {
            FileData.FILE_PATH_KEY put path.toString()
        }
        directory(path, pathToName, formatResolver)
    }
}

@OptIn(DFInternal::class)
@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataDirectory(
    path: Path,
    noinline pathToName: (Path) -> Name = defaultPathToName,
    noinline formatResolver: FileFormatResolver<T>,
): LegacyDataTree<T> = readDataDirectory(typeOf<T>(), path, pathToName, formatResolver)

/**
 * Read a raw binary data tree from the directory. All files are read as-is (save for meta files).
 */
@DFExperimental
public fun IOPlugin.readRawDirectory(
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
): LegacyDataTree<Binary> = readDataDirectory(path, pathToName) { _, _ -> IOReader.binary }


private fun Path.toName() = Name(map { NameToken.parse(it.nameWithoutExtension) })

@DFInternal
@DFExperimental
public fun <T : Any> IOPlugin.monitorDataDirectory(
    type: KType,
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
    formatResolver: FileFormatResolver<T>,
): DataSource<T> {
    if (path.fileName.toString().endsWith(".zip")) error("Monitoring not supported for ZipFS")
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataSource(type, context) {
        directory(path, pathToName, formatResolver)
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
                                file(eventPath, pathToName, formatResolver)
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
    noinline pathToName: (Path) -> Name = defaultPathToName,
    noinline formatResolver: FileFormatResolver<T>,
): DataSource<T> = monitorDataDirectory(typeOf<T>(), path, pathToName, formatResolver)

/**
 * Read and monitor raw binary data tree from the directory. All files are read as-is (save for meta files).
 */
@DFExperimental
public fun IOPlugin.monitorRawDirectory(
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
): DataSource<Binary> = monitorDataDirectory(path, pathToName) { _, _ -> IOReader.binary }

/**
 * Write the data tree to existing directory or create a new one using default [java.nio.file.FileSystem] provider
 *
 * @param nameToPath a [Name] to [Path] converter used to create
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeDataDirectory(
    path: Path,
    dataSet: DataSet<T>,
    format: IOWriter<T>,
    envelopeFormat: EnvelopeFormat? = null,
    nameToPath: (name: Name, data: Data<T>) -> Path = { name, _ ->
        Path(name.tokens.joinToString("/") { token -> token.toStringUnescaped() })
    },
) {
    withContext(Dispatchers.IO) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        } else if (!Files.isDirectory(path)) {
            error("Can't write a node into file")
        }
        dataSet.forEach { (name, data) ->
            val childPath = path.resolve(nameToPath(name, data))
            childPath.parent.createDirectories()
            val envelope = data.toEnvelope(format)
            if (envelopeFormat != null) {
                writeEnvelopeFile(childPath, envelope, envelopeFormat)
            } else {
                writeEnvelopeDirectory(childPath, envelope)
            }
        }
        val directoryMeta = dataSet.meta
        writeMetaFile(path, directoryMeta)
    }
}

/**
 * Reads the specified resources and returns a [LegacyDataTree] containing the data.
 *
 * @param resources The names of the resources to read.
 * @param classLoader The class loader to use for loading the resources. By default, it uses the current thread's context class loader.
 * @return A DataTree containing the data read from the resources.
 */
@DFExperimental
public fun IOPlugin.readResources(
    vararg resources: String,
    pathToName: (Path) -> Name = defaultPathToName,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): LegacyDataTree<Binary> = GenericDataTree {
    resources.forEach { resource ->
        val path = classLoader.getResource(resource)?.toURI()?.toPath() ?: error(
            "Resource with name $resource is not resolved"
        )
        node(resource, readRawDirectory(path, pathToName))
    }
}

/**
 * Add file/directory-based data tree item
 */
context(IOPlugin)
@OptIn(DFInternal::class)
@DFExperimental
public fun <T : Any> DataSetBuilder<T>.file(
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
    formatResolver: FileFormatResolver<out T>,
) {

    try {
        //If path is a single file or a special directory, read it as single datum
        if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
            val data = readDataFile(path, formatResolver)
            if (data == null) {
                logger.warn { "File format is not resolved for $path. Skipping." }
                return
            }
            val name: Name = data.meta[Envelope.ENVELOPE_NAME_KEY].string?.parseAsName() ?: pathToName(path.last())
            data(name, data)
        } else {
            //otherwise, read as directory
            val data: LegacyDataTree<T> = readDataDirectory(dataType, path, pathToName, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string?.parseAsName() ?: pathToName(path.last())
            node(name, data)
        }
    } catch (ex: Exception) {
        logger.error(ex) { "Failed to read file or directory at $path: ${ex.message}" }
    }
}

