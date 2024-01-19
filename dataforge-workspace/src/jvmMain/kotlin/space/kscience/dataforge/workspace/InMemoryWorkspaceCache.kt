package space.kscience.dataforge.workspace

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

private typealias TaskResultId = Pair<Name, Meta>


public class InMemoryWorkspaceCache : WorkspaceCache {

    // never do that at home!
    private val cache = HashMap<TaskResultId, HashMap<Name, TaskData<*>>>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> TaskData<*>.checkType(taskType: KType): TaskData<T> =
        if (type.isSubtypeOf(taskType)) this as TaskData<T>
        else error("Cached data type mismatch: expected $taskType but got $type")

    override suspend fun <T : Any> cache(result: TaskResult<T>): TaskResult<T> {
        for (d: TaskData<T> in result) {
            cache.getOrPut(result.taskName to result.taskMeta) { HashMap() }.getOrPut(d.name) { d }
        }

        return object : TaskResult<T> by result {
            override fun iterator(): Iterator<TaskData<T>> = (cache[result.taskName to result.taskMeta]
                ?.values?.map { it.checkType<T>(result.dataType) }
                ?: emptyList()).iterator()

            override fun get(name: Name): TaskData<T>? {
                val cached: TaskData<*> = cache[result.taskName to result.taskMeta]?.get(name) ?: return null
                //TODO check types
                return cached.checkType(result.dataType)
            }
        }
    }
}

public fun WorkspaceBuilder.inMemoryCache(): Unit = cache(InMemoryWorkspaceCache())