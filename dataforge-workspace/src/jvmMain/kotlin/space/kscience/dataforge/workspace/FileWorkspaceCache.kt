package space.kscience.dataforge.workspace

import kotlinx.io.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.invoke
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.named
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.withIndex
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.reflect.KType

public class JsonIOFormat<T>(public val serializer: KSerializer<T>) : IOFormat<T> {
    override fun readFrom(source: Source): T = Json.decodeFromString(serializer, source.readString())

    override fun writeTo(sink: Sink, obj: T) {
        sink.writeString(Json.encodeToString(serializer, obj))
    }
}

/**
 * An [IOFormat] based on Protobuf representation of the serializeable object.
 */
@OptIn(ExperimentalSerializationApi::class)
public class ProtobufIOFormat<T>(public val serializer: KSerializer<T>) : IOFormat<T> {
    override fun readFrom(source: Source): T = ProtoBuf.decodeFromByteArray(serializer, source.readByteArray())

    override fun writeTo(sink: Sink, obj: T) {
        sink.write(ProtoBuf.encodeToByteArray(serializer, obj))
    }
}

public interface IOFormatResolveStrategy {
    public fun <T> resolve(type: KType, meta: Meta): IOFormat<T>

    public companion object {
        public val PROTOBUF: IOFormatResolveStrategy = object : IOFormatResolveStrategy {
            @Suppress("UNCHECKED_CAST")
            override fun <T> resolve(
                type: KType,
                meta: Meta
            ): IOFormat<T> = ProtobufIOFormat(serializer(type) as KSerializer<T>)
        }

        public val JSON: IOFormatResolveStrategy = object : IOFormatResolveStrategy {
            @Suppress("UNCHECKED_CAST")
            override fun <T> resolve(
                type: KType,
                meta: Meta
            ): IOFormat<T> = JsonIOFormat(serializer(type) as KSerializer<T>)
        }
    }
}

public class FileWorkspaceCache(
    public val cacheDirectory: Path,
    private val ioFormatResolveStrategy: IOFormatResolveStrategy,
) : WorkspaceCache {


    @OptIn(DFExperimental::class, UnsafeKType::class)
    override suspend fun <T> cache(result: TaskResult<T>): TaskResult<T> {
        val io = result.workspace.context.request(IOPlugin)

        val format: IOFormat<T> = ioFormatResolveStrategy.resolve<T>(result.dataType, result.taskMeta)


        val cachingAction: Action<T, T> = CachingAction(result.dataType) { data ->
            val path = cacheDirectory /
                    result.taskName.withIndex(result.taskMeta.hashCode().toString(16)).toString() /
                    data.name.toString()

            val datum: Data<T> = Data<T>(data.type, meta = data.meta, dependencies = data.dependencies) {
                // return cached data if it is present
                if (path.exists()) {
                    try {
                        val envelope: Envelope = io.readEnvelopeFile(path)
                        if (envelope.meta != data.meta) error("Wrong metadata in cached result file")
                        return@Data (envelope.data ?: Binary.EMPTY).readWith(format)
                    } catch (ex: Exception) {
                        result.workspace.logger.error { "Failed to read data from cache: ${ex.localizedMessage}" }
                        //cleanup cache file
                        path.deleteIfExists()
                    }
                }

                //waiting for data in the current scope because Envelope is synchronous
                return@Data data.await().also { result ->
                    val envelope = Envelope {
                        meta = data.meta
                        data {
                            writeWith(format, result)
                        }
                    }
                    io.writeEnvelopeFile(path, envelope)
                }

            }
            datum.named(data.name)
        }

        val cachedTree = cachingAction(result)

        return result.workspace.wrapResult(cachedTree, result.taskName, result.taskMeta)
    }
}

public fun WorkspaceBuilder.fileCache(
    cacheDir: Path,
    ioFormatResolveStrategy: IOFormatResolveStrategy = IOFormatResolveStrategy.PROTOBUF
): Unit = cache(FileWorkspaceCache(cacheDir, ioFormatResolveStrategy))