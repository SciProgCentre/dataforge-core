package hep.dataforge.scripting

import hep.dataforge.context.Global
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.workspace.SimpleWorkspaceBuilder
import hep.dataforge.workspace.context
import hep.dataforge.workspace.target
import org.junit.Test
import kotlin.test.assertEquals


class BuildersKtTest {
    @Test
    fun checkBuilder(){
        val workspace = SimpleWorkspaceBuilder(Global).apply {
            println("I am working")

            context("test")

            target("testTarget"){
                "a" put 12
            }
        }
    }

    @Test
    fun testWorkspaceBuilder() {
        val script = """
            println("I am working")

            context("test")

            target("testTarget"){
                "a" put 12
            }
        """.trimIndent()
        val workspace = Builders.buildWorkspace(script)

        val target = workspace.targets.getValue("testTarget")
        assertEquals(12, target["a"]!!.int)
    }
}