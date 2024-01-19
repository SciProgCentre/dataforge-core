@file:OptIn(ExperimentalCoroutinesApi::class)

package space.kscience.dataforge.workspace

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import kotlin.test.Test
import kotlin.test.assertEquals

class DataPropagationTestPlugin : WorkspacePlugin() {
    override val tag: PluginTag = Companion.tag

    val allData by task<Int> {
        val selectedData = workspace.data.filterByType<Int>()
        val result: Data<Int> = selectedData.foldToData(0) { result, data ->
            result + data.value
        }
        emit("result", result)
    }


    val singleData by task<Int> {
        workspace.data.filterByType<Int>()["myData[12]"]?.let {
            emit("result", it)
        }
    }


    companion object : PluginFactory<DataPropagationTestPlugin> {


        override fun build(context: Context, meta: Meta): DataPropagationTestPlugin = DataPropagationTestPlugin()

        override val tag: PluginTag = PluginTag("Test")
    }
}

class DataPropagationTest {
    val testWorkspace = Workspace {
        context {
            plugin(DataPropagationTestPlugin)
        }
        data {
            repeat(100) {
                static("myData[$it]", it)
            }
        }
    }

    @Test
    fun testAllData() = runTest {
        val node = testWorkspace.produce("Test.allData")
        assertEquals(4950, node.asSequence().single().await())
    }

    @Test
    fun testSingleData() = runTest {
        val node = testWorkspace.produce("Test.singleData")
        assertEquals(12, node.asSequence().single().await())
    }
}