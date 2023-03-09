package space.kscience.dataforge.workspace

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

public class FileWorkspaceCache(public val cacheDirectory: Path) : WorkspaceCache {

    private fun <T : Any> TaskData<*>.checkType(taskType: KType): TaskData<T> = this as TaskData<T>


    @OptIn(DFExperimental::class, DFInternal::class)
    override suspend fun <T : Any> evaluate(result: TaskResult<T>): TaskResult<T> {
        val io = result.workspace.context.request(IOPlugin)

        val format: IOFormat<T> = io.resolveIOFormat(result.dataType, result.taskMeta)
            ?: error("Can't resolve IOFormat for ${result.dataType}")

        fun cachedDataPath(dataName: Name): Path = cacheDirectory /
                result.taskName.withIndex(result.taskMeta.hashCode().toString(16)).toString() /
                dataName.toString()

        fun evaluateDatum(data: TaskData<T>): TaskData<T> {
            val path = cachedDataPath(data.name)
            val datum: Data<T> = Data<T>(data.type, meta = data.meta, dependencies = data.dependencies) {
                // return cached data if it is present
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

                //waiting for data in current scope because Envelope is synchronous
                return@Data data.await().also { result ->
                    val envelope = Envelope {
                        meta = data.meta
                        data {
                            writeObject(format, result)
                        }
                    }
                    io.writeEnvelopeFile(path, envelope)
                }

            }
            return data.workspace.wrapData(datum, data.name, data.taskName, data.taskMeta)
        }

        return object : TaskResult<T> by result {
            override fun iterator(): Iterator<TaskData<T>> =
                iterator().asSequence().map { evaluateDatum(it) }.iterator()

            override fun get(name: Name): TaskData<T>? = result[name]?.let { evaluateDatum(it) }
        }
    }
}