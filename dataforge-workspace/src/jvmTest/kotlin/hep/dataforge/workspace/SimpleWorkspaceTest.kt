package hep.dataforge.workspace

import hep.dataforge.data.first
import hep.dataforge.data.get
import org.junit.Test
import kotlin.test.assertEquals


class SimpleWorkspaceTest {
    val workspace = SimpleWorkspace.build {

        repeat(100) {
            static("myData[$it]", it)
        }


        task("square") {
            model {
                allData()
            }
            pipe<Int, Int> { data ->
                context.logger.info { "Starting square on $data" }
                data * data
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
            joinByGroup<Int, Double> { context ->
                group("even", filter = { name, data -> name.toString().toInt() % 2 == 0 }) {
                    result { data ->
                        context.logger.info { "Starting even" }
                        data.values.average()
                    }
                }
                group("odd", filter = { name, data -> name.toString().toInt() % 2 == 1 }) {
                    result { data ->
                        context.logger.info { "Starting odd" }
                        data.values.average()
                    }
                }
            }
        }

        task("delta"){
            model{
                dependsOn("average")
            }
            join<Double,Double> {data->
                data["even"]!! - data["odd"]!!
            }
        }
    }

    @Test
    fun testWorkspace() {
        val node = workspace.run("sum")
        val res = node.first()
        assertEquals(328350, res.get())
    }
}