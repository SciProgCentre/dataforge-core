package space.kscience.dataforge.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.io.EnvelopeFormat
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.IOWriter
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.exists
import kotlin.io.path.extension

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
