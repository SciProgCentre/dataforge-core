package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataTree
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
public inline fun <reified T : Any> TaskResultScope<*>.dataByType(namePattern: Name? = null): DataSelector<T> =
    DataSelector<T> { workspace, _ ->
        workspace.data.filterByType { name, _, _ ->
            namePattern == null || name.matches(namePattern)
        }
    }

public suspend inline fun <reified T : Any> TaskResultScope<*>.fromTask(
    task: Name,
    taskMeta: Meta = Meta.EMPTY,
): DataTree<T> = workspace.produce(task, taskMeta).filterByType()