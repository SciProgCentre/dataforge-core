package space.kscience.dataforge.workspace

import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.toName
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class DataPropagationTestPlugin : WorkspacePlugin() {
    override val tag: PluginTag = Companion.tag

    val allData by task<Int> {
        val selectedData = workspace.data.select<Int>()
        val result: Data<Int> = selectedData.flow().foldToData(0) { result, data ->
            result + data.await()
        }
        emit("result", result)
    }


    val singleData by task<Int> {
        workspace.data.select<Int>().getData("myData[12]".toName())?.let {
            emit("result", it)
        }
    }


    companion object : PluginFactory<DataPropagationTestPlugin> {

        override val type: KClass<out DataPropagationTestPlugin> = DataPropagationTestPlugin::class

        override fun invoke(meta: Meta, context: Context): DataPropagationTestPlugin = DataPropagationTestPlugin()

        override val tag: PluginTag = PluginTag("Test")
    }
}

class DataPropagationTest {
    val testWorkspace = Workspace {
        context {
            plugin(DataPropagationTestPlugin)
        }
        runBlocking {
            data {
                repeat(100) {
                    static("myData[$it]", it)
                }
            }
        }
    }

    @Test
    fun testAllData() {
        runBlocking {
            val node = testWorkspace.produce("Test.allData")
            assertEquals(4950, node.flow().single().await())
        }
    }

    @Test
    fun testSingleData() {
        runBlocking {
            val node = testWorkspace.produce("Test.singleData")
            assertEquals(12, node.flow().single().await())
        }
    }
}