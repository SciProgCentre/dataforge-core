package hep.dataforge.workspace

import hep.dataforge.context.PluginTag
import hep.dataforge.data.*
import hep.dataforge.meta.boolean
import hep.dataforge.meta.get
import hep.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class SimpleWorkspaceTest {
    val testPlugin = object : WorkspacePlugin() {
        override val tag: PluginTag = PluginTag("test")

        val contextTask = Workspace.task("test") {
            pipe<Any, Unit> {
                context.logger.info { "Test: $it" }
            }
        }
        override val tasks: Collection<Task<*>> = listOf(contextTask)
    }

    val workspace = SimpleWorkspace.build {

        context {
            plugin(testPlugin)
        }

        repeat(100) {
            static("myData[$it]", it)
        }


        val square = task("square") {
            model {
                allData()
            }
            pipe<Int, Int> { data ->
                if (meta["testFlag"].boolean == true) {
                    println("flag")
                }
                context.logger.info { "Starting square on $data" }
                data * data
            }
        }

        val linear = task("linear") {
            model {
                allData()
            }
            pipe<Int, Int> { data ->
                context.logger.info { "Starting linear on $data" }
                data * 2 + 1
            }
        }

        val fullSquare = task("fullsquare") {
            model {
                dependsOn("square", placement = "square".asName())
                dependsOn("linear", placement = "linear".asName())
            }
            transform<Any> { data ->
                val squareNode = data["square"].withType<Int>().node!!
                val linearNode = data["linear"].withType<Int>().node!!
                return@transform DataNode.build(Int::class) {
                    squareNode.dataSequence().forEach { (name, _) ->
                        val newData = Data{
                            val squareValue = squareNode[name].data!!.get()
                            val linearValue = linearNode[name].data!!.get()
                            squareValue+linearValue
                        }
                        set(name,newData)
                    }
                }
            }
        }

        task("sum") {
            model {
                dependsOn("square")
            }
            join<Int, Int> { data ->
                context.logger.info { "Starting sum" }
                data.values.sum()
            }
        }

        task("average") {
            model {
                allData()
            }
            joinByGroup<Int, Double> { env ->
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
                dependsOn("average")
            }
            join<Double, Double> { data ->
                data["even"]!! - data["odd"]!!
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
    fun testFullSquare(){
        val node = workspace.run("fullsquare")
        println(node.toMeta())
    }
}