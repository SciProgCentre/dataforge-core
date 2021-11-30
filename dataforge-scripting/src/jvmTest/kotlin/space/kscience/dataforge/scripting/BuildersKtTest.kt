package space.kscience.dataforge.scripting

import space.kscience.dataforge.context.Global
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.target
import kotlin.test.Test
import kotlin.test.assertEquals


class BuildersKtTest {
    @Test
    fun checkBuilder() {
        Workspace(Global){
            println("I am working")

            //context { name("test") }

            target("testTarget") {
                "a" put 12
            }
        }
    }

    @Test
    fun testWorkspaceBuilder() {
        val script = """
            println("I am working")

            target("testTarget"){
                "a" put 12
            }
        """.trimIndent()
        val workspace = Builders.buildWorkspace(script)

        val target = workspace.targets.getValue("testTarget")
        assertEquals(12, target["a"]!!.int)
    }
}