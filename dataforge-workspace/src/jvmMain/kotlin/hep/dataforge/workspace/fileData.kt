package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTreeBuilder
import hep.dataforge.data.datum
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.io.*
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.nio.asInput
import kotlinx.io.nio.asOutput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.reflect.KClass

/**
 * Read meta from file in a given [MetaFormat]
 */
fun MetaFormat.readMetaFile(path: Path, descriptor: NodeDescriptor? = null): Meta {
    return Files.newByteChannel(path, StandardOpenOption.READ)
        .asInput()
        .readMeta(descriptor)
}

/**
 * Write meta to file using given [MetaFormat]
 */
fun MetaFormat.writeMetaFile(path: Path, meta: Meta, descriptor: NodeDescriptor? = null) {
    return Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
        .asOutput()
        .writeMeta(meta, descriptor)
}

/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 * @param type explicit type of data read
 * @param dataFormat binary format
 * @param envelopeFormatFactory the format of envelope. If null, file is read directly
 * @param metaFile the relative file for optional meta override
 * @param metaFileFormat the meta format for override
 */
fun <T : Any> IOPlugin.readData(
    path: Path,
    type: KClass<out T>,
    dataFormat: IOFormat<T>,
    envelopeFormatFactory: EnvelopeFormatFactory? = null,
    metaFile: Path = path.resolveSibling("${path.fileName}.meta"),
    metaFileFormat: MetaFormat = JsonMetaFormat.default
): Data<T> {
    val externalMeta = if (Files.exists(metaFile)) {
        metaFileFormat.readMetaFile(metaFile)
    } else {
        null
    }
    return if (envelopeFormatFactory == null) {
        Data(type, externalMeta ?: EmptyMeta) {
            withContext(Dispatchers.IO) {
                dataFormat.run {
                    Files.newByteChannel(path, StandardOpenOption.READ)
                        .asInput()
                        .readThis()
                }
            }
        }
    } else {
        readEnvelopeFile(path, envelopeFormatFactory).let {
            if (externalMeta == null) {
                it
            } else {
                it.withMetaLayers(externalMeta)
            }
        }.toData(type, dataFormat)
    }
}

//TODO wants multi-receiver
fun <T : Any> DataTreeBuilder<T>.file(
    plugin: IOPlugin,
    path: Path,
    dataFormat: IOFormat<T>,
    envelopeFormatFactory: EnvelopeFormatFactory? = null
) {
    plugin.run {
        val data = readData(path, type, dataFormat, envelopeFormatFactory)
        val name = path.fileName.toString().replace(".df", "")
        datum(name, data)
    }
}

/**
 * Read the directory as a data node
 */
fun <T : Any> IOPlugin.readDataNode(
    path: Path,
    type: KClass<out T>,
    dataFormat: IOFormat<T>,
    envelopeFormatFactory: EnvelopeFormatFactory? = null
): DataNode<T> {
    if (!Files.isDirectory(path)) error("Provided path $this is not a directory")
    return DataNode(type) {
        Files.list(path).forEach { path ->
            if (!path.fileName.toString().endsWith(".meta")) {
                file(this@readDataNode,path, dataFormat, envelopeFormatFactory)
            }
        }
    }
}

//suspend fun <T : Any> Path.writeData(
//    data: Data<T>,
//    format: IOFormat<T>,
//    )