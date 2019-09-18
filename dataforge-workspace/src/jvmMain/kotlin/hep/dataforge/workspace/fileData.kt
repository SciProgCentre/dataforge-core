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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.reflect.KClass

/**
 * Read meta from file in a given [format]
 */
fun Path.readMeta(format: MetaFormat, descriptor: NodeDescriptor? = null): Meta {
    return format.run {
        Files.newByteChannel(this@readMeta, StandardOpenOption.READ)
            .asInput()
            .readMeta(descriptor)
    }
}

/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * The operation is blocking since it must read meta header. The reading of envelope body is lazy
 * @param type explicit type of data read
 * @param format binary format
 * @param envelopeFormat the format of envelope. If null, file is read directly
 * @param metaFile the relative file for optional meta override
 * @param metaFileFormat the meta format for override
 */
fun <T : Any> Path.readData(
    type: KClass<out T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat? = null,
    metaFile: Path = resolveSibling("$fileName.meta"),
    metaFileFormat: MetaFormat = JsonMetaFormat
): Data<T> {
    val externalMeta = if (Files.exists(metaFile)) {
        metaFile.readMeta(metaFileFormat)
    } else {
        null
    }
    return if (envelopeFormat == null) {
        Data(type, externalMeta ?: EmptyMeta) {
            withContext(Dispatchers.IO) {
                format.run {
                    Files.newByteChannel(this@readData, StandardOpenOption.READ)
                        .asInput()
                        .readThis()
                }
            }
        }
    } else {
        readEnvelope(envelopeFormat).let {
            if (externalMeta == null) {
                it
            } else {
                it.withMetaLayers(externalMeta)
            }
        }.toData(type, format)
    }
}

fun <T : Any> DataTreeBuilder<T>.file(path: Path, format: IOFormat<T>, envelopeFormat: EnvelopeFormat? = null) {
    val data = path.readData(type, format, envelopeFormat)
    val name = path.fileName.toString().replace(".df", "")
    datum(name, data)
}

/**
 * Read the directory as a data node
 */
fun <T : Any> Path.readDataNode(
    type: KClass<out T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat? = null
): DataNode<T> {
    if (!Files.isDirectory(this)) error("Provided path $this is not a directory")
    return DataNode(type) {
        Files.list(this@readDataNode).forEach { path ->
            if (!path.fileName.toString().endsWith(".meta")) {
                file(path, format, envelopeFormat)
            }
        }
    }
}

//suspend fun <T : Any> Path.writeData(
//    data: Data<T>,
//    format: IOFormat<T>,
//    )