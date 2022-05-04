package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.filterIsInstance
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.matches

//public fun WorkspaceBuilder.data(builder: DataSetBuilder<Any>.() -> Unit): Unit = runBlocking {
//    data(builder)
//}

public inline fun <reified T : Any> TaskResultBuilder<*>.data(namePattern: Name? = null): DataSelector<T> =
    object : DataSelector<T> {
        override suspend fun select(workspace: Workspace, meta: Meta): DataSet<T> =
            workspace.data.filterIsInstance { name, _ ->
                namePattern == null || name.matches(namePattern)
            }
    }

public suspend inline fun <reified T : Any> TaskResultBuilder<*>.fromTask(
    task: Name,
    taskMeta: Meta = Meta.EMPTY,
): DataSet<T> = workspace.produce(task, taskMeta).filterIsInstance()