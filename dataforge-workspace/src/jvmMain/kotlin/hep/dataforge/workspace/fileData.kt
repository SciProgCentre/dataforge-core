package hep.dataforge.workspace

//import jdk.nio.zipfs.ZipFileSystemProvider
import hep.dataforge.data.*
import hep.dataforge.io.*
import hep.dataforge.meta.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.io.asOutput
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.spi.FileSystemProvider
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.streams.toList

//public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

public interface FileFormatResolver<T : Any> {
    public val type: KType
    public operator fun invoke(path: Path, meta: Meta): IOFormat<T>
}

@PublishedApi
internal inline fun <reified T : Any> IOPlugin.formatResolver(): FileFormatResolver<T> =
    object : FileFormatResolver<T> {
        override val type: KType = typeOf<T>()

        override fun invoke(path: Path, meta: Meta): IOFormat<T> =
            resolveIOFormat<T>() ?: error("Can't resolve IO format for ${T::class}")
    }

private val <T : Any> FileFormatResolver<T>.kClass: KClass<T>
    get() = type.classifier as? KClass<T> ?: error("Format resolver actual type does not correspond to type parameter")

private fun newZFS(path: Path): FileSystem {
    val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
        ?: error("Zip file system provider not found")
    return fsProvider.newFileSystem(path, mapOf("create" to "true"))
}

/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 * @param type explicit type of data read
 * @param dataFormat binary format
 * @param envelopeFormat the format of envelope. If null, file is read directly
 * @param metaFile the relative file for optional meta override
 * @param metaFileFormat the meta format for override
 */
@DFExperimental
public fun <T : Any> IOPlugin.readDataFile(
    path: Path,
    formatResolver: FileFormatResolver<T>,
): Data<T> {
    val envelope = readEnvelopeFile(path, true) ?: error("Can't read data from $path")
    val format = formatResolver(path, envelope.meta)
    return envelope.toData(format)
}

@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataFile(path: Path): Data<T> = readDataFile(path, formatResolver())

/**
 * Add file/directory-based data tree item
 */
@DFExperimental
public suspend fun <T : Any> DataSetBuilder<T>.file(
    plugin: IOPlugin,
    path: Path,
    formatResolver: FileFormatResolver<T>,
) {
    //If path is a single file or a special directory, read it as single datum
    if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
        plugin.run {
            val data = readDataFile(path, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
                ?: path.fileName.toString().replace(".df", "")
            emit(name, data)
        }
    } else {
        //otherwise, read as directory
        plugin.run {
            val data = readDataDirectory(path, formatResolver)
            val name = data.getMeta()[Envelope.ENVELOPE_NAME_KEY].string
                ?: path.fileName.toString().replace(".df", "")
            emit(name, data)
        }
    }
}

/**
 * Read the directory as a data node. If [path] is a zip archive, read it as directory
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.readDataDirectory(
    path: Path,
    formatResolver: FileFormatResolver<T>,
): DataTree<T> {
    //read zipped data node
    if (path.fileName != null && path.fileName.toString().endsWith(".zip")) {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fs = newZFS(path)
        return readDataDirectory(fs.rootDirectories.first(), formatResolver)
    }
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataTree(formatResolver.kClass) {
        Files.list(path).toList().forEach { path ->
            val fileName = path.fileName.toString()
            if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
                meta(readMetaFile(path))
            } else if (!fileName.startsWith("@")) {
                runBlocking {
                    file(this@readDataDirectory, path, formatResolver)
                }
            }
        }
    }
}

@DFExperimental
public suspend inline fun <reified T : Any> IOPlugin.readDataDirectory(path: Path): DataTree<T> =
    readDataDirectory(path, formatResolver())

/**
 * Write data tree to existing directory or create a new one using default [java.nio.file.FileSystem] provider
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeDataDirectory(
    path: Path,
    tree: DataTree<T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat? = null,
    metaFormat: MetaFormatFactory? = null,
) {
    withContext(Dispatchers.IO) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        } else if (!Files.isDirectory(path)) {
            error("Can't write a node into file")
        }
        tree.items().forEach { (token, item) ->
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
        val treeMeta = tree.getMeta()
        if (treeMeta != null) {
            writeMetaFile(path, treeMeta, metaFormat ?: JsonMetaFormat)
        }
    }
}


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
                envelopeFormat.run {
                    writeObject(asOutput(), envelope)
                }
            }
            is DataTreeItem.Node -> {
                val entry = ZipEntry("$name/")
                putNextEntry(entry)
                closeEntry()
                treeItem.tree.items().forEach { (token, item) ->
                    val childName = "$name/$token"
                    writeNode(childName, item, dataFormat, envelopeFormat)
                }
            }
        }
    }
}

@DFExperimental
suspend fun <T : Any> IOPlugin.writeZip(
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
        val fos = Files.newOutputStream(actualFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)
        val zos = ZipOutputStream(fos)
        zos.use {
            it.writeNode("", DataTreeItem.Node(tree), format, envelopeFormat)
        }

//        if (Files.exists(actualFile) && Files.size(path) == 0.toLong()) {
//            Files.delete(path)
//        }
//        //Files.createFile(actualFile)
//        newZFS(actualFile).use { zipfs ->
//            val zipRootPath = zipfs.getPath("/")
//            Files.createDirectories(zipRootPath)
//            val tmp = Files.createTempDirectory("df_zip")
//            writeDataDirectory(tmp, node, format, envelopeFormat, metaFormat)
//            Files.list(tmp).forEach { sourcePath ->
//                val targetPath = sourcePath.fileName.toString()
//                val internalTargetPath = zipRootPath.resolve(targetPath)
//                Files.copy(sourcePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING)
//            }
//        }
    }
}

