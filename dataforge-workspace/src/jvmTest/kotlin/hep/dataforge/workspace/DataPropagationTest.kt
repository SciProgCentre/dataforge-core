package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.PluginFactory
import hep.dataforge.context.PluginTag
import hep.dataforge.data.*
import hep.dataforge.meta.Meta
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

fun <T : Any> DataSet<T>.first(): NamedData<T>? = runBlocking { flow().firstOrNull() }

class DataPropagationTestPlugin : WorkspacePlugin() {
    override val tag: PluginTag = Companion.tag

    val testAllData = task("allData", Int::class) {
        model {
            data()
        }
        transform<Int> { data ->
            DataTree.dynamic(context) {
                val result = data.flow().map { it.value() }.reduce { acc, pair -> acc + pair }
                data("result", result)
            }
        }
    }


    val testSingleData = task("singleData", Int::class) {
        model {
            data("myData\\[12\\]")
        }
        transform<Int> { data ->
            DataTree.dynamic(context) {
                val result = data.flow().map { it.value() }.reduce { acc, pair -> acc + pair }
                data("result", result)
            }
        }
    }

    val testAllRegexData = task("allRegexData", Int::class) {
        model {
            data(pattern = "myData.*")
        }
        transform<Int> { data ->
            DataTree.dynamic(context) {
                val result = data.flow().map { it.value() }.reduce { acc, pair -> acc + pair }
                data("result", result)
            }
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
        data {
            repeat(100) {
                data("myData[$it]", it)
            }
        }
    }

    @Test
    fun testAllData() {
        runBlocking {
            val node = testWorkspace.run("Test.allData")
            assertEquals(4950, node.first()!!.value())
        }
    }

    @Test
    fun testAllRegexData() {
        runBlocking {
            val node = testWorkspace.run("Test.allRegexData")
            assertEquals(4950, node.first()!!.value())
        }
    }

    @Test
    fun testSingleData() {
        runBlocking {
            val node = testWorkspace.run("Test.singleData")
            assertEquals(12, node.first()!!.value())
        }
    }
}