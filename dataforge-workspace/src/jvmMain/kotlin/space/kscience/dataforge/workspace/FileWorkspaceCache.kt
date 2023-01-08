package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.await
import space.kscience.dataforge.io.*
import space.kscience.dataforge.names.Name
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.reflect.KType

public class FileWorkspaceCache : WorkspaceCache {

    private fun <T : Any> TaskData<*>.checkType(taskType: KType): TaskData<T> = this as TaskData<T>


    override suspend fun <T : Any> evaluate(result: TaskResult<T>): TaskResult<T> {
        val io = result.workspace.context.request(IOPlugin)

        val format: IOFormat<T> = io.resolveIOFormat<T>(result.dataType, result.taskMeta)
            ?: error("Can't resolve IOFormat for ${result.dataType}")

        fun cachedDataPath(dataName: Name): Path {
            TODO()
        }

        fun cachedData(data: TaskData<T>): TaskData<T> {
            val path = cachedDataPath(data.name)
            val cachedData: Data<T> = Data<T>(data.type, meta = data.meta, dependencies = data.dependencies) {
                if (path.exists()) {
                    try {
                        val envelope: Envelope = io.readEnvelopeFile(path)
                        if (envelope.meta != data.meta) error("Wrong metadata in cached result file")
                        return@Data envelope.data?.readWith(format)
                            ?: error("Can't convert envelope without data to Data")
                    } catch (ex: Exception) {
                        //cleanup cache file
                        path.deleteIfExists()
                    }
                }

                return@Data data.await().also {
                    val envelope = Envelope {
                        meta = data.meta
                        data {
                            writeObject(format, it)
                        }
                    }
                    io.writeEnvelopeFile(path, envelope)
                }

            }
            return data.workspace.wrapData(cachedData, data.name, data.taskName, data.taskMeta)
        }

        return object : TaskResult<T> by result {
            override fun iterator(): Iterator<TaskData<T>> = iterator {
                result.iterator().forEach {
                    yield(cachedData(it))
                }
            }

            override fun get(name: Name): TaskData<T>? = result[name]?.let { cachedData(it) }
        }
    }
}