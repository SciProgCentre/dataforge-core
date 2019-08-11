package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.io.*
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

/**
 * Read data with supported envelope format and binary format. If envelope format is null, then read binary directly from file.
 * @param type explicit type of data read
 * @param format binary format
 * @param envelopeFormat the format of envelope. If null, file is read directly
 * @param metaFile the relative file for optional meta override
 * @param metaFileFormat the meta format for override
 */
suspend fun <T : Any> Path.readData(
    type: KClass<out T>,
    format: IOFormat<T>,
    envelopeFormat: EnvelopeFormat? = null,
    metaFile: Path = resolveSibling("$fileName.meta"),
    metaFileFormat: MetaFormat = JsonMetaFormat
): Data<T> {
    return coroutineScope {
        val externalMeta = if (Files.exists(metaFile)) {
            metaFile.readMeta(metaFileFormat)
        } else {
            null
        }
        if (envelopeFormat == null) {
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
            withContext(Dispatchers.IO) {
                readEnvelope(envelopeFormat).let {
                    if (externalMeta == null) {
                        it
                    } else {
                        it.withMetaLayers(externalMeta)
                    }
                }.toData(type, format)
            }
        }
    }
}

//suspend fun <T : Any> Path.writeData(
//    data: Data<T>,
//    format: IOFormat<T>,
//    )