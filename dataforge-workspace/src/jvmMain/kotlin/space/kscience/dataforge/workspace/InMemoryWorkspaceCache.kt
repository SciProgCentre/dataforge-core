package space.kscience.dataforge.workspace

import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.invoke
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.named
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
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

    @OptIn(DFExperimental::class)
    override suspend fun <T> cache(result: TaskResult<T>): TaskResult<T> {
        val cachingAction: Action<T, T> = CachingAction(result.dataType) { data ->
            val cachedData =  cache.getOrPut(TaskResultId(result.taskName, result.taskMeta)){
                HashMap()
            }.getOrPut(data.name){
                data.data
            }
            cachedData.checkType<T>(result.dataType).named(data.name)
        }

        val cachedTree = cachingAction(result)

        return result.workspace.wrapResult(cachedTree, result.taskName, result.taskMeta)
    }
}

public fun WorkspaceBuilder.inMemoryCache(): Unit = cache(InMemoryWorkspaceCache())