package hep.dataforge.workspace

//import jdk.nio.zipfs.ZipFileSystemProvider
import hep.dataforge.data.*
import hep.dataforge.io.*
import hep.dataforge.meta.*
import kotlinx.coroutines.Dispatchers
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

public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

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
    type: KClass<out T>,
    formatResolver: FileFormatResolver<T>
): Data<T> {
    val envelope = readEnvelopeFile(path, true) ?: error("Can't read data from $path")
    val format = formatResolver(path, envelope.meta)
    return envelope.toData(type, format)
}

@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataFile(path: Path): Data<T> =
    readDataFile(path, T::class) { _, _ ->
        resolveIOFormat<T>() ?: error("Can't resolve IO format for ${T::class}")
    }

/**
 * Add file/directory-based data tree item
 */
@DFExperimental
public fun <T : Any> DataTreeBuilder<T>.file(
    plugin: IOPlugin,
    path: Path,
    formatResolver: FileFormatResolver<T>
) {
    //If path is a single file or a special directory, read it as single datum
    if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
        plugin.run {
            val data = readDataFile(path, type, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
                ?: path.fileName.toString().replace(".df", "")
            datum(name, data)
        }
    } else {
        //otherwise, read as directory
        plugin.run {
            val data = readDataDirectory(path, type, formatResolver)
            val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
                ?: path.fileName.toString().replace(".df", "")
            node(name, data)
        }
    }
}

/**
 * Read the directory as a data node. If [path] is a zip archive, read it as directory
 */
@DFExperimental
public fun <T : Any> IOPlugin.readDataDirectory(
    path: Path,
    type: KClass<out T>,
    formatResolver: FileFormatResolver<T>
): DataNode<T> {
    //read zipped data node
    if (path.fileName != null && path.fileName.toString().endsWith(".zip")) {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fs = newZFS(path)
        return readDataDirectory(fs.rootDirectories.first(), type, formatResolver)
    }
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataNode(type) {
        Files.list(path).forEach { path ->
            val fileName = path.fileName.toString()
            if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
                meta(readMetaFile(path))
            } else if (!fileName.startsWith("@")) {
                file(this@readDataDirectory, path, formatResolver)
            }
        }
    }
}

@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataDirectory(path: Path): DataNode<T> =
    readDataDirectory(path, T::class) { _, _ ->
        resolveIOFormat<T>() ?: error("Can't resolve IO format for ${T::class}")
    }

/**
 * Write data tree to existing directory or create a new one using default [java.nio.file.FileSystem] provider
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeDataDirectory(
    path: Path,
    node: DataNode<T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat? = null,
    metaFormat: MetaFormatFactory? = null
) {
    withContext(Dispatchers.IO) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        } else if (!Files.isDirectory(path)) {
            error("Can't write a node into file")
        }
        node.items.forEach { (token, item) ->
            val childPath = path.resolve(token.toString())
            when (item) {
                is DataItem.Node -> {
                    writeDataDirectory(childPath, item.node, format, envelopeFormat)
                }
                is DataItem.Leaf -> {
                    val envelope = item.data.toEnvelope(format)
                    if (envelopeFormat != null) {
                        writeEnvelopeFile(childPath, envelope, envelopeFormat, metaFormat)
                    } else {
                        writeEnvelopeDirectory(childPath, envelope, metaFormat ?: JsonMetaFormat)
                    }
                }
            }
        }
        if (!node.meta.isEmpty()) {
            writeMetaFile(path, node.meta, metaFormat ?: JsonMetaFormat)
        }
    }
}


private suspend fun <T : Any> ZipOutputStream.writeNode(
    name: String,
    item: DataItem<T>,
    dataFormat: IOFormat<T>,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat
) {
    withContext(Dispatchers.IO) {
        when (item) {
            is DataItem.Leaf -> {
                //TODO add directory-based envelope writer
                val envelope = item.data.toEnvelope(dataFormat)
                val entry = ZipEntry(name)
                putNextEntry(entry)
                envelopeFormat.run {
                    writeObject(asOutput(), envelope)
                }
            }
            is DataItem.Node -> {
                val entry = ZipEntry("$name/")
                putNextEntry(entry)
                closeEntry()
                item.node.items.forEach { (token, item) ->
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
    node: DataNode<T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat
) {
    withContext(Dispatchers.IO) {
        val actualFile = if (path.toString().endsWith(".zip")) {
            path
        } else {
            path.resolveSibling(path.fileName.toString() + ".zip")
        }
        val fos = Files.newOutputStream(actualFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        val zos = ZipOutputStream(fos)
        zos.use {
            it.writeNode("", DataItem.Node(node), format, envelopeFormat)
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

