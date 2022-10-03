package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.filterByType
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.matches

//public fun WorkspaceBuilder.data(builder: DataSetBuilder<Any>.() -> Unit): Unit = runBlocking {
//    data(builder)
//}

/**
 * Select the whole data set from the workspace filtered by type.
 */
@OptIn(DFExperimental::class)
public inline fun <reified T : Any> TaskResultBuilder<*>.data(namePattern: Name? = null): DataSelector<T> =
    object : DataSelector<T> {
        override suspend fun select(workspace: Workspace, meta: Meta): DataSet<T> =
            workspace.data.filterByType { name, _ ->
                namePattern == null || name.matches(namePattern)
            }
    }

public suspend inline fun <reified T : Any> TaskResultBuilder<*>.fromTask(
    task: Name,
    taskMeta: Meta = Meta.EMPTY,
): DataSet<T> = workspace.produce(task, taskMeta).filterByType()