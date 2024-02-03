package space.kscience.dataforge.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension


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
): Unit = withContext(Dispatchers.IO) {
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    } else if (!Files.isDirectory(path)) {
        error("Can't write a node into file")
    }
    dataSet.forEach { (name, data) ->
        val childPath = path.resolve(name.tokens.joinToString("/") { token -> token.toStringUnescaped() })
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

/**
 * Write this [DataTree] as a zip archive
 */
@DFExperimental
public suspend fun <T : Any> IOPlugin.writeZip(
    path: Path,
    dataSet: DataTree<T>,
    format: IOWriter<T>,
    envelopeFormat: EnvelopeFormat? = null,
): Unit = withContext(Dispatchers.IO) {
    if (path.exists()) error("Can't override existing zip data file $path")
    val actualFile = if (path.extension == "zip") {
        path
    } else {
        path.resolveSibling(path.fileName.toString() + ".zip")
    }
    val fsProvider = FileSystemProvider.installedProviders().find { it.scheme == "jar" }
        ?: error("Zip file system provider not found")
    //val fs = FileSystems.newFileSystem(actualFile, mapOf("create" to true))
    val fs = fsProvider.newFileSystem(actualFile, mapOf("create" to true))
    fs.use {
        writeDataDirectory(fs.rootDirectories.first(), dataSet, format, envelopeFormat)
    }
}