@file:Suppress("UNUSED_VARIABLE")
@file:OptIn(ExperimentalCoroutinesApi::class)

package space.kscience.dataforge.workspace

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.context.*
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.get
import space.kscience.dataforge.names.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds


/**
 * Make a fake-factory for a one single plugin. Useful for unique or test plugins
 */
public fun <P : Plugin> P.toFactory(): PluginFactory<P> = object : PluginFactory<P> {
    override fun build(context: Context, meta: Meta): P = this@toFactory

    override val tag: PluginTag = this@toFactory.tag
}

public fun Workspace.produceBlocking(task: String, block: MutableMeta.() -> Unit = {}): DataTree<*> = runBlocking {
    produce(task, block).content
}

@OptIn(DFExperimental::class)
internal object TestPlugin : WorkspacePlugin() {
    override val tag: PluginTag = PluginTag("test")

    val test by task {
        // type is inferred
        transformEach(dataByType<Int>()) {
            logger.info { "Test: $value" }
            value
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
                value("myData[$it]", it)
            }
        }

        val filterOne by task<Int> {
            val name by taskMeta.string { error("Name field not defined") }
            result(from(testPluginFactory) { test }[name]!!)
        }

        val square by task<Int> {
            transformEach(dataByType<Int>()) {
                if (meta["testFlag"].boolean == true) {
                    println("Side effect")
                }
                workspace.logger.info { "Starting square on $name" }
                value * value
            }
        }

        val linear by task<Int> {
            transformEach(dataByType<Int>()) {
                workspace.logger.info { "Starting linear on $name" }
                value * 2 + 1
            }
        }

        val fullSquare by task<Int> {
            val squareData = from(square)
            val linearData = from(linear)
            result {
                squareData.forEach { data ->
                    val newData: Data<Int> = data.combine(linearData[data.name]!!) { l, r ->
                        l + r
                    }
                    data(data.name, newData)
                }
            }
        }

        val sum by task<Int> {
            workspace.logger.info { "Starting sum" }
            val res = from(square).foldToData(0) { l, r ->
                l + r.value
            }
            result(res)
        }

        val averageByGroup by task<Int> {
            val evenSum = workspace.data.filterByType<Int> { name, _, _ ->
                name.toString().toInt() % 2 == 0
            }.foldToData(0) { l, r ->
                l + r.value
            }

            val oddSum = workspace.data.filterByType<Int> { name, _, _ ->
                name.toString().toInt() % 2 == 1
            }.foldToData(0) { l, r ->
                l + r.value
            }
            result {
                data("even", evenSum)
                data("odd", oddSum)
            }
        }

        val delta by task<Int> {
            val averaged = from(averageByGroup)
            val even = averaged["event"]!!
            val odd = averaged["odd"]!!
            val res = even.combine(odd) { l, r ->
                l - r
            }
            result(res)
        }

        val customPipe by task<Int> {
            result {
                workspace.data.filterByType<Int>().forEach { data ->
                    val meta = data.meta.toMutableMeta().apply {
                        "newValue" put 22
                    }
                    data(data.name + "new", data.transform { (data.meta["value"].int ?: 0) + it })
                }
            }
        }

        target("empty") {}
    }

    @Test
    fun testWorkspace() = runTest(timeout = 500.milliseconds) {
        val node = workspace.produce("sum")
        val res = node.data
        assertEquals(328350, res?.await())
    }

    @Test
    fun testMetaPropagation() = runTest(timeout = 200.milliseconds) {
        val node = workspace.produce("sum") { "testFlag" put true }
        val res = node.data?.await()
    }

    @Test
    fun testPluginTask() {
        val tasks = workspace.tasks
        assertTrue { tasks["test.test"] != null }
        //val node = workspace.run("test.test", "empty")
    }

    @Test
    fun testFullSquare() = runTest {
        val result = workspace.produce("fullSquare")
        result.forEach {
            println(
                """
                Name: ${it.name}
                Meta: ${it.meta}
                Data: ${it.await()}
            """.trimIndent()
            )
        }
    }

    @Test
    fun testFilter() = runTest {
        val node = workspace.produce("filterOne") {
            "name" put "myData[12]"
        }
        assertEquals(12, node.data?.await())
    }

}