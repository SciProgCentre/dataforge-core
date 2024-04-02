package space.kscience.dataforge.workspace

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

private data class TaskResultId(val name: Name, val meta: Meta)


public class InMemoryWorkspaceCache : WorkspaceCache {

    private val cache = HashMap<TaskResultId, HashMap<Name, Data<*>>>()

    @Suppress("UNCHECKED_CAST")
    private fun <T> Data<*>.checkType(taskType: KType): Data<T> =
        if (type.isSubtypeOf(taskType)) this as Data<T>
        else error("Cached data type mismatch: expected $taskType but got $type")

    override suspend fun <T> cache(result: TaskResult<T>): TaskResult<T> {
        fun cacheOne(data: NamedData<T>): NamedData<T> {
            val cachedData =  cache.getOrPut(TaskResultId(result.taskName, result.taskMeta)){
                HashMap()
            }.getOrPut(data.name){
                data.data
            }
            return cachedData.checkType<T>(result.dataType).named(data.name)
        }


        val cachedTree = result.asSequence().map { cacheOne(it) }
            .toTree(result.dataType, result.updates.filterIsInstance<NamedData<T>>().map { cacheOne(it) })

        return result.workspace.wrapResult(cachedTree, result.taskName, result.taskMeta)
    }
}

public fun WorkspaceBuilder.inMemoryCache(): Unit = cache(InMemoryWorkspaceCache())