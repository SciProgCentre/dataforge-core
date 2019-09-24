package hep.dataforge.workspace

import hep.dataforge.context.PluginTag
import hep.dataforge.data.*
import hep.dataforge.meta.boolean
import hep.dataforge.meta.builder
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.names.plus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class SimpleWorkspaceTest {
    val testPlugin = object : WorkspacePlugin() {
        override val tag: PluginTag = PluginTag("test")

        val contextTask = task("test", Any::class) {
            pipe<Any> {
                context.logger.info { "Test: $it" }
            }
        }
    }

    val workspace = Workspace {

        context {
            plugin(testPlugin)
        }

        data {
            repeat(100) {
                static("myData[$it]", it)
            }
        }


        val square = task<Int>("square") {
            pipe<Int> { data ->
                if (meta["testFlag"].boolean == true) {
                    println("flag")
                }
                context.logger.info { "Starting square on $data" }
                data * data
            }
        }

        val linear = task<Int>("linear") {
            pipe<Int> { data ->
                context.logger.info { "Starting linear on $data" }
                data * 2 + 1
            }
        }

        val fullSquare = task<Int>("fullsquare") {
            model {
                val squareDep = dependsOn(square, placement = "square")
                val linearDep = dependsOn(linear, placement = "linear")
            }
            transform { data ->
                val squareNode = data["square"].node!!.cast<Int>()//squareDep()
                val linearNode = data["linear"].node!!.cast<Int>()//linearDep()
                return@transform DataNode(Int::class) {
                    squareNode.dataSequence().forEach { (name, _) ->
                        val newData = Data {
                            val squareValue = squareNode[name].data!!.get()
                            val linearValue = linearNode[name].data!!.get()
                            squareValue + linearValue
                        }
                        set(name, newData)
                    }
                }
            }
        }

        task<Int>("sum") {
            model {
                dependsOn(square)
            }
            join<Int> { data ->
                context.logger.info { "Starting sum" }
                data.values.sum()
            }
        }

        val average = task<Double>("average") {
            joinByGroup<Int> { env ->
                group("even", filter = { name, _ -> name.toString().toInt() % 2 == 0 }) {
                    result { data ->
                        env.context.logger.info { "Starting even" }
                        data.values.average()
                    }
                }
                group("odd", filter = { name, _ -> name.toString().toInt() % 2 == 1 }) {
                    result { data ->
                        env.context.logger.info { "Starting odd" }
                        data.values.average()
                    }
                }
            }
        }

        task("delta") {
            model {
                dependsOn(average)
            }
            join<Double> { data ->
                data["even"]!! - data["odd"]!!
            }
        }

        val customPipeTask = task<Int>("custom") {
            customPipe<Int> {
                meta = meta.builder().apply {
                    "newValue" to 22
                }
                name += "new"
                result {
                    meta["value"].int ?: 0 + it
                }
            }
        }

        target("empty") {}
    }

    @Test
    fun testWorkspace() {
        val node = workspace.run("sum")
        val res = node.first()
        assertEquals(328350, res?.get())
    }

    @Test
    fun testMetaPropagation() {
        val node = workspace.run("sum") { "testFlag" to true }
        val res = node.first()?.get()
    }

    @Test
    fun testPluginTask() {
        val tasks = workspace.tasks
        assertTrue { tasks["test.test"] != null }
        //val node = workspace.run("test.test", "empty")
    }

    @Test
    fun testFullSquare() {
        val node = workspace.run("fullsquare")
        println(node.toMeta())
    }
}