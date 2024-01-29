package space.kscience.dataforge.workspace

import kotlinx.coroutines.*
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.copy
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.FileData.defaultPathToName
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.*
import kotlin.reflect.typeOf


//public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

public typealias FileFormatResolver<T> = (path: Path, meta: Meta) -> IOReader<T>?


public object FileData {
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


/**
 * Read data with supported envelope format and binary format. If the envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read the meta header. The reading of envelope body is lazy
 */
@OptIn(DFExperimental::class)
public fun IOPlugin.readFileData(
    path: Path,
): Data<Binary> {
    val envelope = readEnvelopeFile(path, true)
    val updatedMeta = envelope.meta.copy {
        FileData.FILE_PATH_KEY put path.toString()
        FileData.FILE_EXTENSION_KEY put path.extension

        val attributes = path.readAttributes<BasicFileAttributes>()
        FileData.FILE_UPDATE_TIME_KEY put attributes.lastModifiedTime().toInstant().toString()
        FileData.FILE_CREATE_TIME_KEY put attributes.creationTime().toInstant().toString()
    }
    return StaticData(
        typeOf<Binary>(),
        envelope.data ?: Binary.EMPTY,
        updatedMeta
    )
}

public fun DataSink<Binary>.file(io: IOPlugin, path: Path, name: Name) {
    if (!path.isRegularFile()) error("Only regular files could be handled by this function")
    data(name, io.readFileData(path))
}

public fun DataSink<Binary>.directory(
    io: IOPlugin,
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
) {
    if (!path.isDirectory()) error("Only directories could be handled by this function")
    val metaFile = path.resolve(IOPlugin.META_FILE_NAME)
    val dataFile = path.resolve(IOPlugin.DATA_FILE_NAME)
    //process root data
    if (metaFile.exists() || dataFile.exists()) {
        data(
            Name.EMPTY,
            StaticData(
                typeOf<Binary>(),
                dataFile.takeIf { it.exists() }?.asBinary() ?: Binary.EMPTY,
                io.readMetaFileOrNull(metaFile) ?: Meta.EMPTY
            )
        )
    }
    Files.list(path).forEach { childPath ->
        val fileName = childPath.fileName.toString()
        if (!fileName.startsWith("@")) {
            files(io, childPath, pathToName)
        }
    }
}

public fun DataSink<Binary>.files(io: IOPlugin, path: Path, pathToName: (Path) -> Name = defaultPathToName) {
    if (path.isRegularFile() && path.extension == "zip") {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
            ?: error("Zip file system provider not found")
        val fs = fsProvider.newFileSystem(path, mapOf("create" to "true"))

        return files(io, fs.rootDirectories.first(), pathToName)
    }
    if (path.isRegularFile()) {
        file(io, path, pathToName(path))
    } else {
        directory(io, path, pathToName)
    }
}


private fun Path.toName() = Name(map { NameToken.parse(it.nameWithoutExtension) })

@DFInternal
@DFExperimental
public fun DataSink<Binary>.monitorFiles(
    io: IOPlugin,
    path: Path,
    pathToName: (Path) -> Name = defaultPathToName,
    scope: CoroutineScope = io.context,
): Job {
    files(io, path, pathToName)
    return scope.launch(Dispatchers.IO) {
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
                        data(eventPath.toName(), null)
                    } else {
                        val fileName = eventPath.fileName.toString()
                        if (!fileName.startsWith("@")) {
                            files(io, eventPath, pathToName)
                        }
                    }
                }
                key.reset()
            }
        } while (isActive && key != null)
    }

}

/**
 * Write the data tree to existing directory or create a new one using default [java.nio.file.FileSystem] provider
 *
 * @param nameToPath a [Name] to [Path] converter used to create
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeDataDirectory(
    path: Path,
    dataSet: DataTree<T>,
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
        dataSet.meta?.let { writeMetaFile(path, it) }

    }
}

/**
 * @param resources The names of the resources to read.
 * @param classLoader The class loader to use for loading the resources. By default, it uses the current thread's context class loader.
 */
@DFExperimental
public fun DataSink<Binary>.resources(
    io: IOPlugin,
    vararg resources: String,
    pathToName: (Path) -> Name = defaultPathToName,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
) {
    resources.forEach { resource ->
        val path = classLoader.getResource(resource)?.toURI()?.toPath() ?: error(
            "Resource with name $resource is not resolved"
        )
        branch(resource.asName()) {
            files(io, path, pathToName)
        }
    }
}
