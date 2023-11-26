package space.kscience.dataforge.workspace

import kotlinx.io.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.await
import space.kscience.dataforge.io.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.withIndex
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.reflect.KType

public class JsonIOFormat<T : Any>(override val type: KType) : IOFormat<T> {

    @Suppress("UNCHECKED_CAST")
    private val serializer: KSerializer<T> = serializer(type) as KSerializer<T>

    override fun readFrom(source: Source): T = Json.decodeFromString(serializer, source.readString())

    override fun writeTo(sink: Sink, obj: T) {
        sink.writeString(Json.encodeToString(serializer, obj))
    }
}

@OptIn(ExperimentalSerializationApi::class)
public class ProtobufIOFormat<T : Any>(override val type: KType) : IOFormat<T> {

    @Suppress("UNCHECKED_CAST")
    private val serializer: KSerializer<T> = serializer(type) as KSerializer<T>

    override fun readFrom(source: Source): T = ProtoBuf.decodeFromByteArray(serializer, source.readByteArray())

    override fun writeTo(sink: Sink, obj: T) {
        sink.write(ProtoBuf.encodeToByteArray(serializer, obj))
    }
}


public class FileWorkspaceCache(public val cacheDirectory: Path) : WorkspaceCache {

    //    private fun <T : Any> TaskData<*>.checkType(taskType: KType): TaskData<T> = this as TaskData<T>

    @OptIn(DFExperimental::class, DFInternal::class)
    override suspend fun <T : Any> evaluate(result: TaskResult<T>): TaskResult<T> {
        val io = result.workspace.context.request(IOPlugin)

        val format: IOFormat<T> = io.resolveIOFormat(result.dataType, result.taskMeta)
            ?: ProtobufIOFormat(result.dataType)
            ?: error("Can't resolve IOFormat for ${result.dataType}")

        fun evaluateDatum(data: TaskData<T>): TaskData<T> {

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

                //waiting for data in current scope because Envelope is synchronous
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
            return data.workspace.wrapData(datum, data.name, data.taskName, data.taskMeta)
        }

        return object : TaskResult<T> by result {
            override fun iterator(): Iterator<TaskData<T>> =
                result.iterator().asSequence().map { evaluateDatum(it) }.iterator()

            override fun get(name: Name): TaskData<T>? = result[name]?.let { evaluateDatum(it) }
        }
    }
}

public fun WorkspaceBuilder.fileCache(cacheDir: Path): Unit = cache(FileWorkspaceCache(cacheDir))