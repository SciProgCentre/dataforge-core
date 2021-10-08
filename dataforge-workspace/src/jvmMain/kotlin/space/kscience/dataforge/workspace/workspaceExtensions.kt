package space.kscience.dataforge.workspace

import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataSetBuilder
import space.kscience.dataforge.data.select
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public fun WorkspaceBuilder.data(builder: suspend DataSetBuilder<Any>.() -> Unit): Unit = runBlocking {
    buildData(builder)
}

public suspend inline fun <reified T : Any> TaskResultBuilder<*>.from(
    task: Name,
    taskMeta: Meta = Meta.EMPTY,
): DataSet<T> = workspace.produce(task, taskMeta).select()