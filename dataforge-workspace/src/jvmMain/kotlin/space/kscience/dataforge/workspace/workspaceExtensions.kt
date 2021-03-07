package space.kscience.dataforge.workspace

import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.data.DataSetBuilder

public fun WorkspaceBuilder.data(builder: suspend DataSetBuilder<Any>.() -> Unit): Unit = runBlocking {
    buildData(builder)
}