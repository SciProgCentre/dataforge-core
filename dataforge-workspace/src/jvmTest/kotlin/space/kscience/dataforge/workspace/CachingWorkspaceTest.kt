package space.kscience.dataforge.workspace

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import space.kscience.dataforge.data.startAll
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.boolean
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.misc.DFExperimental

@OptIn(ExperimentalCoroutinesApi::class, DFExperimental::class)
internal class CachingWorkspaceTest {
    private val workspace = Workspace {
        data {
            //statically initialize data
            repeat(5) {
                static("myData[$it]", it)
            }
        }

        useCache()

        val doFirst by task<Any> {
            pipeFrom(data()) { _, name, _ ->
                println("Done first on $name with flag=${taskMeta["flag"].boolean ?: false}")
            }
        }

        @Suppress("UNUSED_VARIABLE") val doSecond by task<Any>{
            pipeFrom(doFirst) { _, name, _ ->
                println("Done second on $name with flag=${taskMeta["flag"].boolean ?: false}")
            }
        }
    }


    @Test
    fun testMetaPropagation() = runTest {
        val first = workspace.produce("doFirst")
        val secondA = workspace.produce("doSecond")
        val secondB = workspace.produce("doSecond", Meta { "flag" put true })
        first.startAll(this)
        secondA.startAll(this)
        secondB.startAll(this)
    }

}