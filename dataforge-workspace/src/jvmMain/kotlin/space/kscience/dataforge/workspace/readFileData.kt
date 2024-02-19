package space.kscience.dataforge.workspace

import kotlinx.coroutines.*
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataSink
import space.kscience.dataforge.data.StaticData
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.copy
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
import kotlin.io.path.*
import kotlin.reflect.typeOf


public object FileData {
    public val FILE_KEY: Name = "file".asName()
    public val FILE_PATH_KEY: Name = FILE_KEY + "path"
    public val FILE_EXTENSION_KEY: Name = FILE_KEY + "extension"
    public val FILE_CREATE_TIME_KEY: Name = FILE_KEY + "created"
    public val FILE_UPDATE_TIME_KEY: Name = FILE_KEY + "updated"
    public const val DF_FILE_EXTENSION: String = "df"
    public val DEFAULT_IGNORE_EXTENSIONS: Set<String> = setOf(DF_FILE_EXTENSION)

}


/**
 * Read data with supported envelope format and binary format. If the envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read the meta header. The reading of envelope body is lazy
 */
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

public fun DataSink<Binary>.file(io: IOPlugin, name: Name, path: Path) {
    if (!path.isRegularFile()) error("Only regular files could be handled by this function")
    put(name, io.readFileData(path))
}

public fun DataSink<Binary>.directory(
    io: IOPlugin,
    name: Name,
    path: Path,
) {
    if (!path.isDirectory()) error("Only directories could be handled by this function")
    //process root data

    var dataBinary: Binary? = null
    var meta: Meta? = null
    Files.list(path).forEach { childPath ->
        val fileName = childPath.fileName.toString()
        if (fileName == IOPlugin.DATA_FILE_NAME) {
            dataBinary = childPath.asBinary()
        } else if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
            meta = io.readMetaFileOrNull(childPath)
        } else if (!fileName.startsWith("@")) {
            val token = if (childPath.isRegularFile() && childPath.extension in FileData.DEFAULT_IGNORE_EXTENSIONS) {
                NameToken(childPath.nameWithoutExtension)
            } else {
                NameToken(childPath.name)
            }

            files(io, name + token, childPath)
        }
    }

    //set data if it is relevant
    if (dataBinary != null || meta != null) {
        put(
            name,
            StaticData(
                typeOf<Binary>(),
                dataBinary ?: Binary.EMPTY,
                meta ?: Meta.EMPTY
            )
        )
    }
}

public fun DataSink<Binary>.files(
    io: IOPlugin,
    name: Name,
    path: Path,
) {
    if (path.isRegularFile() && path.extension == "zip") {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
            ?: error("Zip file system provider not found")
        val fs = fsProvider.newFileSystem(path, emptyMap<String, Any>())

        files(io, name, fs.rootDirectories.first())
    }
    if (path.isRegularFile()) {
        file(io, name, path)
    } else {
        directory(io, name, path)
    }
}


private fun Path.toName() = Name(map { NameToken.parse(it.nameWithoutExtension) })

public fun DataSink<Binary>.monitorFiles(
    io: IOPlugin,
    name: Name,
    path: Path,
    scope: CoroutineScope = io.context,
): Job {
    files(io, name, path)
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
                        put(eventPath.toName(), null)
                    } else {
                        val fileName = eventPath.fileName.toString()
                        if (!fileName.startsWith("@")) {
                            files(io, name, eventPath)
                        }
                    }
                }
                key.reset()
            }
        } while (isActive && key != null)
    }

}

/**
 * @param resources The names of the resources to read.
 * @param classLoader The class loader to use for loading the resources. By default, it uses the current thread's context class loader.
 */
public fun DataSink<Binary>.resources(
    io: IOPlugin,
    vararg resources: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
) {
    resources.forEach { resource ->
        val path = classLoader.getResource(resource)?.toURI()?.toPath() ?: error(
            "Resource with name $resource is not resolved"
        )
        files(io, resource.asName(), path)
    }
}
