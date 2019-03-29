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
    }

    @Test
    fun testWorkspace() {
        val node = workspace.run("sum")
        val res = node.first()
        assertEquals(328350, res.get())
    }
}