package hep.dataforge.workspace

import hep.dataforge.data.DataSetBuilder
import kotlinx.coroutines.runBlocking

public fun WorkspaceBuilder.data(builder: suspend DataSetBuilder<Any>.() -> Unit): Unit = runBlocking {
    buildData(builder)
}