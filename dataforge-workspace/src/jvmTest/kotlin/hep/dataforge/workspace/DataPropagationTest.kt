package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.PluginFactory
import hep.dataforge.context.PluginTag
import hep.dataforge.data.*
import hep.dataforge.meta.Meta
import hep.dataforge.names.asName
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals


class DataPropagationTestPlugin : WorkspacePlugin() {
    override val tag: PluginTag = Companion.tag

    val testAllData = task("allData", Int::class) {
        model {
            allData()
        }
        transform<Int> { data ->
            return@transform DataNode {
                val result = data.dataSequence().map { it.second.get() }.reduce { acc, pair -> acc + pair }
                set("result".asName(), Data { result })
            }
        }
    }


    val testSingleData = task("singleData", Int::class) {
        model {
            data("myData\\[12\\]")
        }
        transform<Int> { data ->
            return@transform DataNode {
                val result = data.dataSequence().map { it.second.get() }.reduce { acc, pair -> acc + pair }
                set("result".asName(), Data { result })
            }
        }
    }

    val testAllRegexData = task("allRegexData", Int::class) {
        model {
            data(pattern = "myData.*")
        }
        transform<Int> { data ->
            return@transform DataNode {
                val result = data.dataSequence().map { it.second.get() }.reduce { acc, pair -> acc + pair }
                set("result".asName(), Data { result })
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
                static("myData[$it]", it)
            }
        }
    }

    @Test
    fun testAllData() {
        val node = testWorkspace.run("Test.allData")
        assertEquals(4950, node.first()!!.get())
    }

    @Test
    fun testAllRegexData() {
        val node = testWorkspace.run("Test.allRegexData")
        assertEquals(4950, node.first()!!.get())
    }

    @Test
    fun testSingleData() {
        val node = testWorkspace.run("Test.singleData")
        assertEquals(12, node.first()!!.get())
    }
}