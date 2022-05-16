package space.kscience.dataforge.workspace

import io.ktor.utils.io.streams.asOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.spi.FileSystemProvider
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.streams.toList

//public typealias FileFormatResolver<T> = (Path, Meta) -> IOFormat<T>

public typealias FileFormatResolver<T> = (path: Path, meta: Meta) -> IOReader<T>


private fun newZFS(path: Path): FileSystem {
    val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
        ?: error("Zip file system provider not found")
    return fsProvider.newFileSystem(path, mapOf("create" to "true"))
}

/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 */
@DFExperimental
public inline fun <reified T : Any> IOPlugin.readDataFile(
    path: Path,
    formatResolver: FileFormatResolver<T>,
): Data<T> {
    val envelope = readEnvelopeFile(path, true)
    val format = formatResolver(path, envelope.meta)
    return envelope.toData(format)
}

/**
 * Add file/directory-based data tree item
 */
context(IOPlugin) @DFExperimental
public fun DataSetBuilder<Any>.file(
    path: Path,
    formatResolver: FileFormatResolver<Any>,
) {
    //If path is a single file or a special directory, read it as single datum
    if (!Files.isDirectory(path) || Files.list(path).allMatch { it.fileName.toString().startsWith("@") }) {
        val data = readDataFile(path, formatResolver)
        val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
            ?: path.fileName.toString().replace(".df", "")
        data(name, data)
    } else {
        //otherwise, read as directory
        val data = readDataDirectory(path, formatResolver)
        val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
            ?: path.fileName.toString().replace(".df", "")
        node(name, data)
    }
}

/**
 * Read the directory as a data node. If [path] is a zip archive, read it as directory
 */
@DFExperimental
public fun IOPlugin.readDataDirectory(
    path: Path,
    formatResolver: FileFormatResolver<Any>,
): DataTree<Any> {
    //read zipped data node
    if (path.fileName != null && path.fileName.toString().endsWith(".zip")) {
        //Using explicit Zip file system to avoid bizarre compatibility bugs
        val fs = newZFS(path)
        return readDataDirectory(fs.rootDirectories.first(), formatResolver)
    }
    if (!Files.isDirectory(path)) error("Provided path $path is not a directory")
    return DataTree<Any> {
        Files.list(path).toList().forEach { path ->
            val fileName = path.fileName.toString()
            if (fileName.startsWith(IOPlugin.META_FILE_NAME)) {
                meta(readMetaFile(path))
            } else if (!fileName.startsWith("@")) {
                file(path, formatResolver)
            }
        }
    }
}

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

@Suppress("BlockingMethodInNonBlockingContext")
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeZip(
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
    }
}

