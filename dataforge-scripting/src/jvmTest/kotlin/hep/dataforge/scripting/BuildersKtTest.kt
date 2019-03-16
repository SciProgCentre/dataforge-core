package hep.dataforge.scripting

import hep.dataforge.meta.get
import hep.dataforge.meta.int
import org.junit.Test
import kotlin.test.assertEquals


class BuildersKtTest {
    @Test
    fun testWorkspaceBuilder() {
        val script = """
            println("I am working")

            context{
                name = "test"
            }

            target("testTarget"){
                "a" to 12
            }
        """.trimIndent()
        val workspace = Builders.buildWorkspace(script)

        val target = workspace.targets.getValue("testTarget")
        assertEquals(12, target["a"]!!.int)
    }
}