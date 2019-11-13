package hep.dataforge.workspace

import hep.dataforge.data.*
import hep.dataforge.io.Envelope
import hep.dataforge.io.IOFormat
import hep.dataforge.io.IOPlugin
import hep.dataforge.io.readEnvelopeFile
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass


/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 * @param type explicit type of data read
 * @param dataFormat binary format
 * @param envelopeFormat the format of envelope. If null, file is read directly
 * @param metaFile the relative file for optional meta override
 * @param metaFileFormat the meta format for override
 */
fun <T : Any> IOPlugin.readDataFile(
    path: Path,
    type: KClass<out T>,
    formatResolver: (Meta) -> IOFormat<T>
): Data<T> {
    val envelope = readEnvelopeFile(path, true) ?: error("Can't read data from $path")
    val format = formatResolver(envelope.meta)
    return envelope.toData(type, format)
}

//TODO wants multi-receiver
fun <T : Any> DataTreeBuilder<T>.file(
    plugin: IOPlugin,
    path: Path,
    formatResolver: (Meta) -> IOFormat<T>
) {
    plugin.run {
        val data = readDataFile(path, type, formatResolver)
        val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
            ?: path.fileName.toString().replace(".df", "")
        datum(name, data)
    }
}

/**
 * Read the directory as a data node
 */
fun <T : Any> IOPlugin.readDataDirectory(
    path: Path,
    type: KClass<out T>,
    formatResolver: (Meta) -> IOFormat<T>
): DataNode<T> {
    if (!Files.isDirectory(path)) error("Provided path $this is not a directory")
    return DataNode(type) {
        Files.list(path).forEach { path ->
            if (!path.fileName.toString().endsWith(".meta")) {
                file(this@readDataDirectory, path, formatResolver)
            }
        }
    }
}

fun <T : Any> DataTreeBuilder<T>.directory(
    plugin: IOPlugin,
    path: Path,
    formatResolver: (Meta) -> IOFormat<T>
) {
    plugin.run {
        val data = readDataDirectory(path, type, formatResolver)
        val name = data.meta[Envelope.ENVELOPE_NAME_KEY].string
            ?: path.fileName.toString().replace(".df", "")
        node(name, data)
    }
}





