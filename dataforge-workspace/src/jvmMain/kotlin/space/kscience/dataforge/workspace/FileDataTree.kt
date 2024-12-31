package space.kscience.dataforge.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.StaticData
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.copy
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public class FileDataTree(
    public val io: IOPlugin,
    public val path: Path,
    private val monitor: Boolean = false
) : DataTree<Binary> {
    override val dataType: KType = typeOf<Binary>()

    /**
     * Read data with supported envelope format and binary format. If the envelope format is null, then read binary directly from file.
     * The operation is blocking since it must read the meta header. The reading of envelope body is lazy
     */
    private fun readFileAsData(
        path: Path,
    ): Data<Binary> {
        val envelope = io.readEnvelopeFile(path, true)
        val updatedMeta = envelope.meta.copy {
            FILE_PATH_KEY put path.toString()
            FILE_EXTENSION_KEY put path.extension

            val attributes = path.readAttributes<BasicFileAttributes>()
            FILE_UPDATE_TIME_KEY put attributes.lastModifiedTime().toInstant().toString()
            FILE_CREATE_TIME_KEY put attributes.creationTime().toInstant().toString()
        }
        return StaticData(
            typeOf<Binary>(),
            envelope.data ?: Binary.EMPTY,
            updatedMeta
        )
    }

    private fun readFilesFromDirectory(
        path: Path
    ): Map<NameToken, FileDataTree> = path.listDirectoryEntries().filterNot { it.name.startsWith("@") }.associate {
        NameToken.parse(it.nameWithoutExtension) to FileDataTree(io, it)
    }

    override val data: Data<Binary>?
        get() = when {
            path.isRegularFile() -> {
                //TODO process zip
                readFileAsData(path)
            }

            path.isDirectory() -> {
                val dataBinary: Binary? = path.resolve(IOPlugin.DATA_FILE_NAME)?.asBinary()
                val meta: Meta? = path.find { it.fileName.startsWith(IOPlugin.META_FILE_NAME) }?.let {
                    io.readMetaFileOrNull(it)
                }
                if (dataBinary != null || meta != null) {
                    StaticData(
                        typeOf<Binary>(),
                        dataBinary ?: Binary.EMPTY,
                        meta ?: Meta.EMPTY
                    )
                } else {
                    null
                }
            }

            else -> {
                null
            }
        }


    override val items: Map<NameToken, DataTree<Binary>>
        get() = when {
            path.isDirectory() -> readFilesFromDirectory(path)
            path.isRegularFile() && path.extension == "zip" -> {
                //Using an explicit Zip file system to avoid bizarre compatibility bugs
                val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
                    ?: error("Zip file system provider not found")
                val fs = fsProvider.newFileSystem(path, emptyMap<String, Any>())
                readFilesFromDirectory(fs.rootDirectories.single())
            }

            else -> emptyMap()
        }


    override val updates: Flow<Name> = if (monitor) {
        callbackFlow<Name> {
            val watchService: WatchService = path.fileSystem.newWatchService()

            fun Path.toName() = Name(map { NameToken.parse(it.nameWithoutExtension) })

            fun monitor(childPath: Path): Job {
                val key: WatchKey = childPath.register(
                    watchService, arrayOf(
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE,
                    )
                )

                return launch {
                    while (isActive) {
                        for (event: WatchEvent<*> in key.pollEvents()) {
                            val eventPath = event.context() as Path
                            if (event.kind() === StandardWatchEventKinds.ENTRY_CREATE) {
                                monitor(eventPath)
                            } else {
                                send(eventPath.relativeTo(path).toName())
                            }
                        }
                        key.reset()
                    }
                }
            }

            monitor(path)

            awaitClose {
                watchService.close()
            }

        }.flowOn(Dispatchers.IO).shareIn(io.context, SharingStarted.WhileSubscribed())
    } else {
        emptyFlow()
    }

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


///**
// * @param resources The names of the resources to read.
// * @param classLoader The class loader to use for loading the resources. By default, it uses the current thread's context class loader.
// */
//@DFExperimental
//public fun DataSink<Binary>.resources(
//    io: IOPlugin,
//    resource: String,
//    vararg otherResources: String,
//    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
//) {
//    //create a file system if necessary
//    val uri = Thread.currentThread().contextClassLoader.getResource("common")!!.toURI()
//    try {
//        uri.toPath()
//    } catch (e: FileSystemNotFoundException) {
//        FileSystems.newFileSystem(uri, mapOf("create" to "true"))
//    }
//
//    listOf(resource, *otherResources).forEach { r ->
//        val path = classLoader.getResource(r)?.toURI()?.toPath() ?: error(
//            "Resource with name $r is not resolved"
//        )
//        io.readAsDataTree(r.asName(), path)
//    }
//}
