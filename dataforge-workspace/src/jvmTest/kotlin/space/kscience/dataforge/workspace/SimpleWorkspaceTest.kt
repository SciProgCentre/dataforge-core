@file:Suppress("UNUSED_VARIABLE")
@file:OptIn(ExperimentalCoroutinesApi::class)

package space.kscience.dataforge.workspace

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Timeout
import space.kscience.dataforge.context.*
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.get
import space.kscience.dataforge.names.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Make a fake-factory for a one single plugin. Useful for unique or test plugins
 */
public fun <P : Plugin> P.toFactory(): PluginFactory<P> = object : PluginFactory<P> {
    override fun build(context: Context, meta: Meta): P = this@toFactory

    override val tag: PluginTag = this@toFactory.tag
}

public fun Workspace.produceBlocking(task: String, block: MutableMeta.() -> Unit = {}): DataSet<Any> = runBlocking {
    produce(task, block)
}

@OptIn(DFExperimental::class)
internal object TestPlugin : WorkspacePlugin() {
    override val tag: PluginTag = PluginTag("test")

    val test by task {
        // type is inferred
        transformEach(dataByType<Int>()) { arg, _, _ ->
            logger.info { "Test: $arg" }
            arg
        }

    }

}


@DFExperimental
internal class SimpleWorkspaceTest {

    val testPluginFactory = TestPlugin.toFactory()

    val workspace = Workspace {

        context {
            //load existing task via plugin into the workspace
            plugin(testPluginFactory)
        }

        data {
            //statically initialize data
            repeat(100) {
                static("myData[$it]", it)
            }
        }

        val filterOne by task<Int> {
            val name by taskMeta.string { error("Name field not defined") }
            from(testPluginFactory) { test }.getByType<Int>(name)?.let { source ->
                data(source.name, source.map { it })
            }
        }

        val square by task<Int> {
            transformEach(dataByType<Int>()) { arg, name, meta ->
                if (meta["testFlag"].boolean == true) {
                    println("Side effect")
                }
                workspace.logger.info { "Starting square on $name" }
                arg * arg
            }
        }

        val linear by task<Int> {
            transformEach(dataByType<Int>()) { arg, name, _ ->
                workspace.logger.info { "Starting linear on $name" }
                arg * 2 + 1
            }
        }

        val fullSquare by task<Int> {
            val squareData = from(square)
            val linearData = from(linear)
            squareData.forEach { data ->
                val newData: Data<Int> = data.combine(linearData[data.name]!!) { l, r ->
                    l + r
                }
                data(data.name, newData)
            }
        }

        val sum by task<Int> {
            workspace.logger.info { "Starting sum" }
            val res = from(square).foldToData(0) { l, r ->
                l + r.value
            }
            data("sum", res)
        }

        val averageByGroup by task<Int> {
            val evenSum = workspace.data.filterByType<Int> { name, _ ->
                name.toString().toInt() % 2 == 0
            }.foldToData(0) { l, r ->
                l + r.value
            }

            data("even", evenSum)
            val oddSum = workspace.data.filterByType<Int> { name, _ ->
                name.toString().toInt() % 2 == 1
            }.foldToData(0) { l, r ->
                l + r.value
            }
            data("odd", oddSum)
        }

        val delta by task<Int> {
            val averaged = from(averageByGroup)
            val even = averaged["event"]!!
            val odd = averaged["odd"]!!
            val res = even.combine(odd) { l, r ->
                l - r
            }
            data("res", res)
        }

        val customPipe by task<Int> {
            workspace.data.filterByType<Int>().forEach { data ->
                val meta = data.meta.toMutableMeta().apply {
                    "newValue" put 22
                }
                data(data.name + "new", data.transform { (data.meta["value"].int ?: 0) + it })
            }
        }

        target("empty") {}
    }

    @Test
    @Timeout(1)
    fun testWorkspace() = runTest {
        val node = workspace.produce("sum")
        val res = node.asSequence().single()
        assertEquals(328350, res.await())
    }

    @Test
    @Timeout(1)
    fun testMetaPropagation() = runTest {
        val node = workspace.produce("sum") { "testFlag" put true }
        val res = node.asSequence().single().await()
    }

    @Test
    fun testPluginTask() {
        val tasks = workspace.tasks
        assertTrue { tasks["test.test"] != null }
        //val node = workspace.run("test.test", "empty")
    }

    @Test
    fun testFullSquare() {
        runBlocking {
            val node = workspace.produce("fullSquare")
            println(node.toMeta())
        }
    }

    @Test
    fun testFilter() {
        runBlocking {
            val node = workspace.produce("filterOne") {
                "name" put "myData[12]"
            }
            assertEquals(12, node.single().await())
        }
    }
}