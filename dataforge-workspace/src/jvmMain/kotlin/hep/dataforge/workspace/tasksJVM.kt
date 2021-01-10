package hep.dataforge.workspace

import hep.dataforge.data.DataSet
import hep.dataforge.meta.MetaBuilder
import kotlinx.coroutines.runBlocking

public fun Workspace.runBlocking(task: String, block: MetaBuilder.() -> Unit = {}): DataSet<Any> = runBlocking{
    run(task, block)
}