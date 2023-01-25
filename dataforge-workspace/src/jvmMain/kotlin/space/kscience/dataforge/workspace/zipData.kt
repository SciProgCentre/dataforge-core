package space.kscience.dataforge.workspace

import io.ktor.utils.io.streams.asOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.io.EnvelopeFormat
import space.kscience.dataforge.io.IOFormat
import space.kscience.dataforge.io.TaggedEnvelopeFormat
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


private suspend fun <T : Any> ZipOutputStream.writeNode(
    name: String,
    treeItem: DataTreeItem<T>,
    dataFormat: IOFormat<T>,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
): Unit = withContext(Dispatchers.IO) {
    when (treeItem) {
        is DataTreeItem.Leaf -> {
            //TODO add directory-based envelope writer
            val envelope = treeItem.data.toEnvelope(dataFormat)
            val entry = ZipEntry(name)
            putNextEntry(entry)
            asOutput().run {
                envelopeFormat.writeObject(this, envelope)
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

/**
 * Write this [DataTree] as a zip archive
 */
@DFExperimental
public suspend fun <T : Any> DataTree<T>.writeZip(
    path: Path,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat = TaggedEnvelopeFormat,
): Unit = withContext(Dispatchers.IO) {
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
        it.writeNode("", DataTreeItem.Node(this@writeZip), format, envelopeFormat)
    }
}