package space.kscience.dataforge.workspace

import io.ktor.utils.io.streams.asOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readAttributes
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.streams.toList


//public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

public typealias FileFormatResolver<T> = (path: Path, meta: Meta) -> IOReader<T>

public object FileData {
    public val META_FILE_KEY: Name = "file".asName()
    public val META_FILE_PATH_KEY: Name = META_FILE_KEY + "path"
    public val META_FILE_EXTENSION_KEY: Name = META_FILE_KEY + "extension"
    public val META_FILE_CREATE_TIME_KEY: Name = META_FILE_KEY + "created"
    public val META_FILE_UPDATE_TIME_KEY: Name = META_FILE_KEY + "update"
}


@DFInternal
@DFExperimental
public fun <T : Any> IOPlugin.readDataFile(
    type: KType,
    path: Path,
    formatResolver: FileFormatResolver<T>,
): Data<T> {
    val envelope = readEnvelopeFile(path, true)
    val format = formatResolver(path, envelope.meta)
    val updatedMeta = envelope.meta.copy {
        FileData.META_FILE_PATH_KEY put path.toString()
        FileData.META_FILE_EXTENSION_KEY put path.extension

        val attributes = path.readAttributes<BasicFileAttributes>()
        FileData.META_FILE_UPDATE_TIME_KEY put attributes.lastModifiedTime().toInstant().toString()
        FileData.META_FILE_CREATE_TIME_KEY put attributes.creationTime().toInstant().toString()
    }
    return Data(type, updatedMeta) {
        envelope.data?.readWith(format) ?: error("Can't convert envelope without content to Data")
    }
}


/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 */
@OptIn(DFInternal::class)
@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataFile(
    path: Path,
    noinline formatResolver: FileFormatResolver<T>,
): Data<T> = readDataFile(typeOf<T>(), path, formatResolver)

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


@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun <T : Any> ZipOutputStream.writeNode(
    name: String,
    treeItem: DataTreeItem<T>,
    dataFormat: IOFormat<T>,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
) {
    withContext(Dispatchers.IO) {
        when (treeItem) {
            is DataTreeItem.Leaf -> {
                //TODO add directory-based envelope writer
                val envelope = treeItem.data.toEnvelope(dataFormat)
                val entry = ZipEntry(name)
                putNextEntry(entry)
                asOutput().run {
                    envelopeFormat.writeEnvelope(this, envelope)
                    flush()
                }
            }
            is DataTreeItem.Node -> {
                val entry = ZipEntry("$name/")
                putNextEntry(entry)
                closeEntry()
                treeItem.tree.items.forEach { (token, item) ->
                    val childName = "$name/$token"
                    writeNode(childName, item, dataFormat, envelopeFormat)
                }
            }
        }
    }
}

@DFExperimental
public suspend fun <T : Any> FileData.writeZip(
    path: Path,
    tree: DataTree<T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
) {
    withContext(Dispatchers.IO) {
        val actualFile = if (path.toString().endsWith(".zip")) {
            path
        } else {
            path.resolveSibling(path.fileName.toString() + ".zip")
        }
        val fos = Files.newOutputStream(
            actualFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        val zos = ZipOutputStream(fos)
        zos.use {
            it.writeNode("", DataTreeItem.Node(tree), format, envelopeFormat)
        }
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
    //If path is a single file or a special directory, read it as single datum
    if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
        val data = readDataFile(dataType, path, formatResolver)
        val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string ?: path.nameWithoutExtension
        data(name, data)
    } else {
        //otherwise, read as directory
        val data = readDataDirectory(dataType, path, formatResolver)
        val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string ?: path.nameWithoutExtension
        node(name, data)
    }
}

