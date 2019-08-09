package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.data.Data
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.io.IOFormat
import hep.dataforge.io.JsonMetaFormat
import hep.dataforge.io.MetaFormat
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.io.nio.asInput
import kotlinx.io.nio.asOutput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.reflect.KClass

/**
 * Read meta from file in a given [format]
 */
suspend fun Path.readMeta(format: MetaFormat, descriptor: NodeDescriptor? = null): Meta {
    return withContext(Dispatchers.IO) {
        format.run {
            Files.newByteChannel(this@readMeta, StandardOpenOption.READ)
                .asInput()
                .readMeta(descriptor)
        }
    }
}

/**
 * Write meta to file in a given [format]
 */
suspend fun Meta.write(path: Path, format: MetaFormat, descriptor: NodeDescriptor? = null) {
    withContext(Dispatchers.IO) {
        format.run {
            Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
                .asOutput()
                .writeMeta(this@write, descriptor)
        }
    }
}

suspend fun <T : Any> Context.readData(
    type: KClass<out T>,
    path: Path,
    format: IOFormat<T>,
    metaFile: Path = path.resolveSibling("${path.fileName}.meta"),
    metaFileFormat: MetaFormat = JsonMetaFormat
): Data<T> {
    return coroutineScope {
        val externalMeta = if (Files.exists(metaFile)) {
            metaFile.readMeta(metaFileFormat)
        } else {
            null
        }
        Data(type, externalMeta ?: EmptyMeta){
            withContext(Dispatchers.IO) {
                format.run {
                    Files.newByteChannel(path, StandardOpenOption.READ)
                        .asInput()
                        .readThis()
                }
            }
        }
    }
}